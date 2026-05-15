package com.yy.catmed.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 处方/医嘱记录
 * 记录某个时间点的医嘱剂量，支持调药历史追踪
 */
@Entity(tableName = "prescriptions")
data class Prescription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,          // 关联的药品ID
    val dosageMgPerKg: Double,       // 每公斤剂量mg
    val startDate: Long,             // 生效日期
    val endDate: Long? = null,       // 结束日期（null表示当前有效）
    val isActive: Boolean = true     // 是否当前生效
)
