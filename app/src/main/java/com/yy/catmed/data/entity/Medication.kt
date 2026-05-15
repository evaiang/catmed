package com.yy.catmed.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 药品/补剂定义
 */
@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // 名称
    val type: String,                    // "drug" 药品 / "supplement" 补剂
    val strengthMg: Double? = null,      // 每片mg数（药品才有）
    val isActive: Boolean = true,        // 是否启用
    val reminderHour: Int = 8,           // 提醒时间-时
    val reminderMinute: Int = 30,        // 提醒时间-分
    val reminderHour2: Int = 20,         // 第二次提醒-时
    val reminderMinute2: Int = 30        // 第二次提醒-分
)
