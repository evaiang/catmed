package com.yy.catmed.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import com.yy.catmed.MainActivity
import com.yy.catmed.R
import com.yy.catmed.data.db.AppDatabase
import com.yy.catmed.data.entity.MedicationRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var medicationId: Long = -1L
    private var medicationName: String = ""
    private var medicationType: String = "drug"
    private var strengthMg: Double = 0.0
    private var slot: Int = 1

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "喂药提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "猫咪喂药提醒服务"
                enableVibration(true)
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        medicationId = intent?.getLongExtra("medication_id", -1L) ?: -1L
        medicationName = intent?.getStringExtra("medication_name") ?: ""
        medicationType = intent?.getStringExtra("medication_type") ?: "drug"
        strengthMg = intent?.getDoubleExtra("strength_mg", 0.0) ?: 0.0
        slot = intent?.getIntExtra("slot", 1) ?: 1

        showForegroundNotification()
        showOverlay()

        return START_NOT_STICKY
    }

    private fun showForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("猫药管家")
                .setContentText("提醒服务运行中")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("猫药管家")
                .setContentText("提醒服务运行中")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun showOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                stopSelf()
                return
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_reminder, null)
        overlayView.setOnTouchListener(OverlayTouchListener(params))

        // 填充药品信息
        overlayView.findViewById<TextView>(R.id.tv_med_name).text = medicationName
        val typeLabel = if (medicationType == "drug") "💊 药品" else "🧪 补剂"
        overlayView.findViewById<TextView>(R.id.tv_med_type).text = typeLabel

        // 已喂药
        overlayView.findViewById<Button>(R.id.btn_taken).setOnClickListener {
            recordTaken()
            dismissOverlay()
        }

        // 稍后提醒
        overlayView.findViewById<Button>(R.id.btn_snooze).setOnClickListener {
            snooze()
            dismissOverlay()
        }

        windowManager.addView(overlayView, params)

        // 30秒后自动关闭
        overlayView.postDelayed({
            if (overlayView.isAttachedToWindow) {
                autoMiss()
                dismissOverlay()
            }
        }, 30000)
    }

    private fun recordTaken() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@FloatingOverlayService)
            val scheduledTime = calculateScheduledTime()
            val record = MedicationRecord(
                medicationId = medicationId,
                scheduledTime = scheduledTime,
                actualTime = System.currentTimeMillis(),
                dosageMg = strengthMg.takeIf { it > 0 },
                status = "taken"
            )
            db.medicationRecordDao().insert(record)

            // 发送广播通知 WebView 刷新
            sendBroadcast(Intent("com.yy.catmed.REFRESH_DATA"))
        }
    }

    private fun snooze() {
        // 5分钟后重新触发
        val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(this, ReminderAlarmReceiver::class.java).apply {
            putExtra("medication_id", medicationId)
            putExtra("slot", slot)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            (medicationId * 10 + slot).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000
        alarmManager.setAlarmClock(
            android.app.AlarmManager.AlarmClockInfo(snoozeTime, null),
            pendingIntent
        )
    }

    private fun autoMiss() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@FloatingOverlayService)
            val record = MedicationRecord(
                medicationId = medicationId,
                scheduledTime = calculateScheduledTime(),
                actualTime = System.currentTimeMillis(),
                status = "missed"
            )
            db.medicationRecordDao().insert(record)
            sendBroadcast(Intent("com.yy.catmed.REFRESH_DATA"))
        }
    }

    private fun calculateScheduledTime(): Long {
        val cal = java.util.Calendar.getInstance()
        val hour = if (slot == 1) 8 else 20
        val minute = if (slot == 1) 30 else 30
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
        cal.set(java.util.Calendar.MINUTE, minute)
        cal.set(java.util.Calendar.SECOND, 0)
        return cal.timeInMillis
    }

    private fun dismissOverlay() {
        try {
            if (::overlayView.isInitialized && overlayView.isAttachedToWindow) {
                windowManager.removeView(overlayView)
            }
        } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            if (::overlayView.isInitialized && overlayView.isAttachedToWindow) {
                windowManager.removeView(overlayView)
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }

    private inner class OverlayTouchListener(private val params: WindowManager.LayoutParams) : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    return true
                }
            }
            return false
        }
    }

    companion object {
        private const val CHANNEL_ID = "medication_reminder_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
