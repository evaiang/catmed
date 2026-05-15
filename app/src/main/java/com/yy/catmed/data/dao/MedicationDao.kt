package com.yy.catmed.data.dao

import androidx.room.*
import com.yy.catmed.data.entity.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY isActive DESC, id ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY id ASC")
    fun getActiveMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY id ASC")
    suspend fun getActiveMedicationsList(): List<Medication>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)
}
