package com.bds89.miner2android.forRoom

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(entities = [PCsEntity::class, LimitEntity::class, CUREntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pcsDao(): PCsDao?
    abstract fun LimitDao(): LimitDao?
    abstract fun CURDao(): CURDao?

    companion object MIGRATION_1_2: Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `CURsUSER` (`id` INTEGER NOT NULL, `CURsymbol` TEXT NOT NULL, PRIMARY KEY(`id`))")
        }
    }
}