package com.yy.catmed.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yy.catmed.data.dao.*
import com.yy.catmed.data.entity.*

@Database(
    entities = [
        Medication::class,
        MedicationRecord::class,
        CatWeight::class,
        Prescription::class,
        ExerciseRecord::class,
        EventType::class,
        EventRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationRecordDao(): MedicationRecordDao
    abstract fun catWeightDao(): CatWeightDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun exerciseRecordDao(): ExerciseRecordDao
    abstract fun eventTypeDao(): EventTypeDao
    abstract fun eventRecordDao(): EventRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "catmed_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
