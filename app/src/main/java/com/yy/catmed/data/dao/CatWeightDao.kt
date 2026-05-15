package com.yy.catmed.data.dao

import androidx.room.*
import com.yy.catmed.data.entity.CatWeight
import kotlinx.coroutines.flow.Flow

@Dao
interface CatWeightDao {
    @Query("SELECT * FROM cat_weights ORDER BY date DESC")
    fun getAllWeights(): Flow<List<CatWeight>>

    @Query("SELECT * FROM cat_weights ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeight(): CatWeight?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weight: CatWeight): Long

    @Delete
    suspend fun delete(weight: CatWeight)
}
