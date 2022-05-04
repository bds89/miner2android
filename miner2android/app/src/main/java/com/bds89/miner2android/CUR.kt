package com.bds89.miner2android

import java.io.Serializable

data class CUR(
    val name:String,
    val symbol:String,
    val price:Double,
    val percent_change_1h:Double,
    val percent_change_24h:Double,
    val market_cap:Double,
    val total_supply:String,
    val max_supply:String,
    val circulating_supply:String,
    val volume_24h:Double,
    val volume_change_24h: Double,
    val cmc_rank:Int,
    val tags:String,
) :Serializable {

}
