package com.yy.catmed.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 喂药记录
 */
@Entity(tableName = "medication_records")
data class MedicationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,          // 关联的药品ID
    val scheduledTime: Long,         // 预定喂药时间（毫秒时间戳）
    val actualTime: Long,            // 实际喂药时间（毫秒时间戳）
    val dosageMg: Double? = null,    // 实际剂量mg
    val note: String = "",           // 备注
    val status: String = "taken"     // "taken" 已喂 / "missed" 漏服 / "snoozed" 推迟
)
