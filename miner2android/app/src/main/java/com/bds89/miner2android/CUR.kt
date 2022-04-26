package com.bds89.miner2android

import java.io.Serializable

data class CUR(
    val name:String,
    val symbol:String,
    val price:Double,
    val percent_change_1h:Double,
    val percent_change_24h:Double,
    val market_cap:Double,
) :Serializable {

}
