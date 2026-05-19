package com.yy.catmed.data.dao

import androidx.room.*
import com.yy.catmed.data.entity.EventType
import kotlinx.coroutines.flow.Flow

@Dao
interface EventTypeDao {
    @Query("SELECT * FROM event_types WHERE isActive = 1 ORDER BY id ASC")
    fun getActiveTypes(): Flow<List<EventType>>

    @Query("SELECT * FROM event_types WHERE isActive = 1 ORDER BY id ASC")
    suspend fun getActiveTypesList(): List<EventType>

    @Query("SELECT * FROM event_types ORDER BY id ASC")
    fun getAllTypes(): Flow<List<EventType>>

    @Query("SELECT * FROM event_types WHERE id = :id")
    suspend fun getById(id: Long): EventType?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: EventType): Long

    @Update
    suspend fun update(type: EventType)

    @Delete
    suspend fun delete(type: EventType)
}
