package com.bds89.miner2android

import java.io.Serializable
import java.security.MessageDigest

data class PC(
    var id:Int = 0,
    var name:String,
    var imageID:Int,
    var status:String = "unknown",
    var ex_IP:String="",
    var port:String,
    var in_IP:String="",
    var in_port:String,
    var visibility: Boolean=false): Serializable {

    var upass = ""
        set(value) {
        val bytes = value.toByteArray()
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(bytes)
        field = digest.joinToString("") { eachByte -> "%02x".format(eachByte) }
        }

    }
