package com.bds89.miner2android.forRoom

import androidx.room.*


@Dao
interface LimitDao {
    @Query("SELECT * FROM limits")
    suspend fun getAll(): List<LimitEntity?>?

    @Query("SELECT * FROM limits WHERE pcName = :pcname")
    suspend fun getByPcname(pcname: String): List<LimitEntity?>?

    @Query("DELETE from limits WHERE pcName = :pname")
    suspend fun deleteByPcname(pname: String): Int

    @Query("DELETE from limits")
    suspend fun deleteAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(limit: LimitEntity?): Long

    @Update
    suspend fun update(limit: LimitEntity?)

    @Delete
    suspend fun delete(limit: LimitEntity?)
}