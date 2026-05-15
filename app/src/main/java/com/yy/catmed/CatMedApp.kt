package com.yy.catmed

import android.app.Application
import com.yy.catmed.data.db.AppDatabase

class CatMedApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
    }
}
