package com.yy.catmed.data.dao

import androidx.room.*
import com.yy.catmed.data.entity.Prescription
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM prescriptions ORDER BY startDate DESC")
    fun getAllPrescriptions(): Flow<List<Prescription>>

    @Query("SELECT * FROM prescriptions WHERE isActive = 1 ORDER BY startDate DESC")
    suspend fun getActivePrescriptions(): List<Prescription>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prescription: Prescription): Long

    @Update
    suspend fun update(prescription: Prescription)

    @Delete
    suspend fun delete(prescription: Prescription)
}
