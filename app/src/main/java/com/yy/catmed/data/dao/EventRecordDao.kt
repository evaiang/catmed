package com.yy.catmed.data.dao

import androidx.room.*
import com.yy.catmed.data.entity.EventRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface EventRecordDao {
    @Query("SELECT * FROM event_records ORDER BY time DESC")
    fun getAllRecords(): Flow<List<EventRecord>>

    @Query("SELECT * FROM event_records WHERE time BETWEEN :startTime AND :endTime ORDER BY time DESC")
    suspend fun getRecordsInRange(startTime: Long, endTime: Long): List<EventRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: EventRecord): Long

    @Delete
    suspend fun delete(record: EventRecord)

    @Query("DELETE FROM event_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
