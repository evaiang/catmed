package com.yy.catmed.data.dao

import androidx.room.*
import com.yy.catmed.data.entity.ExerciseRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseRecordDao {
    @Query("SELECT * FROM exercise_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<ExerciseRecord>>

    @Query("SELECT * FROM exercise_records WHERE date BETWEEN :startOfDay AND :endOfDay ORDER BY date ASC")
    fun getRecordsByDay(startOfDay: Long, endOfDay: Long): Flow<List<ExerciseRecord>>

    @Query("SELECT * FROM exercise_records WHERE date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    suspend fun getRecordsInRange(startTime: Long, endTime: Long): List<ExerciseRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExerciseRecord): Long

    @Delete
    suspend fun delete(record: ExerciseRecord)
}
