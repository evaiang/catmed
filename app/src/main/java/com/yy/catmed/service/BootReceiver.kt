package com.yy.catmed.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yy.catmed.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机后重新注册提醒闹钟
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val meds = db.medicationDao().getActiveMedicationsList()
                for (med in meds) {
                    ReminderAlarmReceiver.scheduleAlarm(context, med, 1)
                    ReminderAlarmReceiver.scheduleAlarm(context, med, 2)
                }
            }
        }
    }
}
