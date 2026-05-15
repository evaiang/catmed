package com.yy.catmed.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yy.catmed.data.db.AppDatabase
import com.yy.catmed.data.entity.Medication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra("medication_id", -1L)
        val slot = intent.getIntExtra("slot", 1) // 1=上午, 2=下午
        if (medicationId == -1L) return

        // 读取药品信息后启动悬浮窗
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val meds = db.medicationDao().getActiveMedicationsList()
            val med = meds.find { it.id == medicationId } ?: return@launch

            val overlayIntent = Intent(context, FloatingOverlayService::class.java).apply {
                putExtra("medication_id", med.id)
                putExtra("medication_name", med.name)
                putExtra("medication_type", med.type)
                putExtra("strength_mg", med.strengthMg ?: 0.0)
                putExtra("slot", slot)
            }
            context.startForegroundService(overlayIntent)
        }
    }

    companion object {
        fun scheduleAlarm(context: Context, medication: Medication, slot: Int) {
            val hour = if (slot == 1) medication.reminderHour else medication.reminderHour2
            val minute = if (slot == 1) medication.reminderMinute else medication.reminderMinute2

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

            val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                putExtra("medication_id", medication.id)
                putExtra("slot", slot)
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                (medication.id * 10 + slot).toInt(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // 计算今天或明天的提醒时间
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                // 如果时间已过，设为明天
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
            }

            alarmManager.setAlarmClock(
                android.app.AlarmManager.AlarmClockInfo(calendar.timeInMillis, null),
                pendingIntent
            )
        }
    }
}
