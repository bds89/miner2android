package com.bds89.miner2android.forRoom

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bds89.miner2android.PC
import java.io.Serializable

@Entity(
    tableName = "limits",
)
data class LimitEntity(
    @PrimaryKey(autoGenerate = true) var id: Long=0,
    var pcName:String,
    val ptype:Int,
    val pname:String,
    var above:Boolean=true,
    var value:Int,
    var datetime:Long
): Serializable  {

}