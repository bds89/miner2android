package com.bds89.miner2android.forRoom

import androidx.room.*


@Dao
interface PCsDao {
    @Query("SELECT * FROM PCs")
    suspend fun getAll(): List<PCsEntity?>?

    @Query("SELECT * FROM PCs WHERE name = :name")
    suspend fun getById(name: String): PCsEntity?

    @Query("DELETE from PCs WHERE id = :name")
    suspend fun deleteByName(name: String): Int

    @Query("DELETE from PCs")
    suspend fun deleteAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pc: PCsEntity?): Long

    @Update
    suspend fun update(pc: PCsEntity?)

    @Delete
    suspend fun delete(pc: PCsEntity?)


}