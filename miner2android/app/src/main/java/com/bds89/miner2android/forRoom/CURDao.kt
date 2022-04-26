package com.bds89.miner2android.forRoom

import androidx.room.*


@Dao
interface CURDao {
    @Query("SELECT * FROM CURsUSER")
    suspend fun getAll(): List<CUREntity?>?

    @Query("DELETE from CURsUSER WHERE CURsymbol = :cur")
    suspend fun deleteBySymbol(cur: String): Int

    @Query("DELETE from CURsUSER")
    suspend fun deleteAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cur: CUREntity?): Long

    suspend fun insertAll(curList:List<String>) {
        deleteAll()
        curList.forEach {
            insert(CUREntity(CURsymbol = it))
        }
    }
}