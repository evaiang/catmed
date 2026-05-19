package com.yy.catmed.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_types")
data class EventType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "pill",
    val color: String = "#0891B2",
    val dailyTarget: Int = 2,
    val reminders: String = "[]",   // JSON array: ["08:30","20:30"]
    val remindOn: Boolean = true,
    val isActive: Boolean = true
)
