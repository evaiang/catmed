package com.yy.catmed.web

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.JavascriptInterface
import com.yy.catmed.data.db.AppDatabase
import com.yy.catmed.data.entity.*
import com.yy.catmed.service.ReminderAlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * JS ↔ Native 桥接接口
 * 所有 @JavascriptInterface 方法均可被 WebView JS 调用
 */
class WebAppInterface(private val context: Context) {

    private val db by lazy { AppDatabase.getInstance(context) }
    private val scope = CoroutineScope(Dispatchers.IO)

    // ========== 药品管理 ==========

    @JavascriptInterface
    fun getMedications(): String {
        return runBlocking {
            val meds = db.medicationDao().getActiveMedicationsList()
            val arr = JSONArray()
            for (m in meds) {
                arr.put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("type", m.type)
                    put("strengthMg", m.strengthMg ?: JSONObject.NULL)
                    put("isActive", m.isActive)
                    put("reminderHour", m.reminderHour)
                    put("reminderMinute", m.reminderMinute)
                    put("reminderHour2", m.reminderHour2)
                    put("reminderMinute2", m.reminderMinute2)
                })
            }
            arr.toString()
        }
    }

    @JavascriptInterface
    fun saveMedication(json: String) {
        scope.launch {
            val obj = JSONObject(json)
            val med = Medication(
                id = obj.optLong("id", 0),
                name = obj.getString("name"),
                type = obj.getString("type"),
                strengthMg = if (obj.has("strengthMg") && !obj.isNull("strengthMg"))
                    obj.getDouble("strengthMg") else null,
                reminderHour = obj.optInt("reminderHour", 8),
                reminderMinute = obj.optInt("reminderMinute", 30),
                reminderHour2 = obj.optInt("reminderHour2", 20),
                reminderMinute2 = obj.optInt("reminderMinute2", 30)
            )
            val id = db.medicationDao().insert(med)
            // 注册提醒
            if (id > 0) {
                val savedMed = med.copy(id = id)
                ReminderAlarmReceiver.scheduleAlarm(context, savedMed, 1)
                ReminderAlarmReceiver.scheduleAlarm(context, savedMed, 2)
            }
        }
    }

    @JavascriptInterface
    fun deleteMedication(id: Long) {
        scope.launch {
            val med = db.medicationDao().getActiveMedicationsList().find { it.id == id }
            if (med != null) {
                db.medicationDao().delete(med)
            }
        }
    }

    // ========== 喂药记录 ==========

    @JavascriptInterface
    fun getMedicationRecords(year: Int, month: Int): String {
        return runBlocking {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1, 0, 0, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val end = cal.timeInMillis

            val records = db.medicationRecordDao().getRecordsInRange(start, end)
            val arr = JSONArray()
            for (r in records) {
                arr.put(JSONObject().apply {
                    put("id", r.id)
                    put("medicationId", r.medicationId)
                    put("scheduledTime", r.scheduledTime)
                    put("actualTime", r.actualTime)
                    put("dosageMg", r.dosageMg ?: JSONObject.NULL)
                    put("note", r.note)
                    put("status", r.status)
                })
            }
            arr.toString()
        }
    }

    @JavascriptInterface
    fun saveMedicationRecord(json: String) {
        scope.launch {
            val obj = JSONObject(json)
            val record = MedicationRecord(
                medicationId = obj.getLong("medicationId"),
                scheduledTime = obj.getLong("scheduledTime"),
                actualTime = obj.getLong("actualTime"),
                dosageMg = if (obj.has("dosageMg") && !obj.isNull("dosageMg"))
                    obj.getDouble("dosageMg") else null,
                note = obj.optString("note", ""),
                status = obj.optString("status", "taken")
            )
            db.medicationRecordDao().insert(record)
        }
    }

    // ========== 体重 ==========

    @JavascriptInterface
    fun getLatestWeight(): String {
        return runBlocking {
            val w = db.catWeightDao().getLatestWeight()
            if (w != null) {
                JSONObject().apply {
                    put("weightKg", w.weightKg)
                    put("date", w.date)
                }.toString()
            } else {
                "{}"
            }
        }
    }

    @JavascriptInterface
    fun saveWeight(json: String) {
        scope.launch {
            val obj = JSONObject(json)
            val weight = CatWeight(
                weightKg = obj.getDouble("weightKg"),
                date = obj.optLong("date", System.currentTimeMillis())
            )
            db.catWeightDao().insert(weight)
        }
    }

    // ========== 医嘱 ==========

    @JavascriptInterface
    fun getActivePrescriptions(): String {
        return runBlocking {
            val prescs = db.prescriptionDao().getActivePrescriptions()
            val arr = JSONArray()
            for (p in prescs) {
                arr.put(JSONObject().apply {
                    put("id", p.id)
                    put("medicationId", p.medicationId)
                    put("dosageMgPerKg", p.dosageMgPerKg)
                    put("startDate", p.startDate)
                })
            }
            arr.toString()
        }
    }

    @JavascriptInterface
    fun savePrescription(json: String) {
        scope.launch {
            val obj = JSONObject(json)
            val p = Prescription(
                medicationId = obj.getLong("medicationId"),
                dosageMgPerKg = obj.getDouble("dosageMgPerKg"),
                startDate = obj.optLong("startDate", System.currentTimeMillis())
            )
            db.prescriptionDao().insert(p)
        }
    }

    // ========== 运动记录 ==========

    @JavascriptInterface
    fun saveExerciseRecord(json: String) {
        scope.launch {
            val obj = JSONObject(json)
            val record = ExerciseRecord(
                date = obj.getLong("date"),
                durationMinutes = obj.optInt("durationMinutes", 30),
                note = obj.optString("note", "")
            )
            db.exerciseRecordDao().insert(record)
        }
    }

    @JavascriptInterface
    fun getExerciseRecords(year: Int, month: Int): String {
        return runBlocking {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1, 0, 0, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val end = cal.timeInMillis

            val records = db.exerciseRecordDao().getRecordsInRange(start, end)
            val arr = JSONArray()
            for (r in records) {
                arr.put(JSONObject().apply {
                    put("id", r.id)
                    put("date", r.date)
                    put("durationMinutes", r.durationMinutes)
                    put("note", r.note)
                })
            }
            arr.toString()
        }
    }

    // ========== 剂量计算 ==========

    @JavascriptInterface
    fun calculateDosage(weightKg: Double, mgPerKg: Double, strengthMg: Double): String {
        val requiredMg = weightKg * mgPerKg        // 需要多少mg
        val pills = requiredMg / strengthMg         // 折合多少片
        // 最接近的分数表示（简化分药）
        val fractions = findSimplestFraction(pills)
        return JSONObject().apply {
            put("requiredMg", Math.round(requiredMg * 100.0) / 100.0)
            put("pills", Math.round(pills * 100.0) / 100.0)
            put("fractionNumerator", fractions.first)
            put("fractionDenominator", fractions.second)
            put("weightKg", weightKg)
            put("mgPerKg", mgPerKg)
            put("strengthMg", strengthMg)
        }.toString()
    }

    // 找最近似的分数（分母不超过8，方便分药）
    private fun findSimplestFraction(value: Double): Pair<Int, Int> {
        val best = (1..8).minBy { denom ->
            val num = Math.round(value * denom).toInt()
            Math.abs(value - num.toDouble() / denom)
        }
        val numerator = Math.round(value * best).toInt()
        val gcd = gcd(numerator, best)
        return Pair(numerator / gcd, best / gcd)
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    // ========== 权限 & 系统 ==========

    @JavascriptInterface
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    @JavascriptInterface
    fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    @JavascriptInterface
    fun hasExactAlarmPermission(): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    @JavascriptInterface
    fun openAlarmSettings() {
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
