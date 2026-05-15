package com.yy.catmed.data.dao

import androidx.room.*
import com.yy.catmed.data.entity.MedicationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationRecordDao {
    @Query("SELECT * FROM medication_records ORDER BY actualTime DESC")
    fun getAllRecords(): Flow<List<MedicationRecord>>

    @Query("SELECT * FROM medication_records WHERE actualTime BETWEEN :startOfDay AND :endOfDay ORDER BY actualTime ASC")
    fun getRecordsByDay(startOfDay: Long, endOfDay: Long): Flow<List<MedicationRecord>>

    @Query("SELECT * FROM medication_records WHERE actualTime BETWEEN :startTime AND :endTime ORDER BY actualTime DESC")
    suspend fun getRecordsInRange(startTime: Long, endTime: Long): List<MedicationRecord>

    @Query("SELECT * FROM medication_records WHERE medicationId = :medId AND actualTime BETWEEN :startTime AND :endTime ORDER BY actualTime DESC")
    suspend fun getRecordsForMedication(medId: Long, startTime: Long, endTime: Long): List<MedicationRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MedicationRecord): Long

    @Update
    suspend fun update(record: MedicationRecord)

    @Delete
    suspend fun delete(record: MedicationRecord)
}
