package com.yy.catmed.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 猫咪体重记录
 */
@Entity(tableName = "cat_weights")
data class CatWeight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Double,      // 体重kg
    val date: Long              // 记录日期（毫秒时间戳）
)
