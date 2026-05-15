package com.yy.catmed.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 运动记录
 */
@Entity(tableName = "exercise_records")
data class ExerciseRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,                     // 日期（毫秒时间戳）
    val durationMinutes: Int = 30,      // 运动时长（分钟）
    val note: String = ""               // 备注
)
