package com.bds89.miner2android.forRoom

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bds89.miner2android.PC
import java.io.Serializable


@Entity(
    tableName = "PCs",
    indices = [
            Index("name", unique = true)
            ]
)
data class PCsEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    val name:String,
    val imageID:Int,
    val status:String = "unknown",
    val ex_IP:String="",
    val port:String,
    val in_IP:String="",
    val in_port:String,
    val visibility: Boolean=false,
    val upass:String
) {
    fun toPC(): PC {
        val pc = PC(
            id = id.toInt(),
            name = name,
            imageID = imageID,
            status = status,
            ex_IP = ex_IP,
            port = port,
            in_IP = in_IP,
            in_port = in_port,
            visibility = visibility,
            up = upass
        )
        return pc
    }

    companion object {
        fun fromPC(pc: PC): PCsEntity = PCsEntity(
            id = pc.id.toLong(),
            name = pc.name,
            imageID = pc.imageID,
            status = pc.status,
            ex_IP = pc.ex_IP,
            port = pc.port,
            in_IP = pc.in_IP,
            in_port = pc.in_port,
            visibility =pc.visibility,
            upass = pc.upass
        )
        fun listToPCList(PClistEntity: List<PCsEntity?>?): ArrayList<PC> {
            if (PClistEntity.isNullOrEmpty()) return ArrayList<PC>()
            else {
                val PClist = arrayListOf<PC>()
                PClistEntity.forEach {
                    if (it == null) return@forEach
                    val pc = PC(
                        id = it.id.toInt(),
                        name = it.name,
                        imageID = it.imageID,
                        status = it.status,
                        ex_IP = it.ex_IP,
                        port = it.port,
                        in_IP = it.in_IP,
                        in_port = it.in_port,
                        visibility = it.visibility,
                        up = it.upass
                    )
                    PClist.add(pc)
                }
                return PClist
            }
        }

    }
}