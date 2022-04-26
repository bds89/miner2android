package com.bds89.miner2android.forRoom

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bds89.miner2android.PC
import java.io.Serializable

@Entity(
    tableName = "CURsUSER",
)
data class CUREntity(
    @PrimaryKey(autoGenerate = true) var id: Long=0,
    var CURsymbol:String,
): Serializable  {
}