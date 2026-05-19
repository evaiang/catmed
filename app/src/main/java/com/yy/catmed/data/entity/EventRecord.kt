package com.yy.catmed.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_records")
data class EventRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val typeId: Long,
    val time: Long,         // 记录时间戳
    val undone: Boolean = false
)
