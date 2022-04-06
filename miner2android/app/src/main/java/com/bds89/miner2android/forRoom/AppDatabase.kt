package com.bds89.miner2android.forRoom

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [PCsEntity::class, LimitEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pcsDao(): PCsDao?
    abstract fun LimitDao(): LimitDao?
}