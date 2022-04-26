package com.bds89.miner2android.forRoom

import android.app.Application
import androidx.room.Room


class App : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(this, AppDatabase::class.java, "database")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    companion object {
        lateinit var instance: App
    }
}