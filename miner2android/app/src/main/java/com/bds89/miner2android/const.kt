package com.bds89.miner2android

import android.content.res.Resources
import android.provider.Settings.Global.getString


object const {
    val KEY_PC_item: String = "KEY_PC_item"
    val KEY_edit_item: String = "KEY_edit_item"
    val KEY_node_Name: String = "KEY_node_Name"
    val KEY_PCList: String = "KEY_PCList"
    val KEY_PosNum: String = "KEY_PosNum"
    val KEY_IconNum: String = "KEY_IconNum"
    val KEY_SavePC: String = "KEY_SavePC"
    val KEY_SaveSettings: String = "KEY_SaveSettings"
    val KEY_SaveLimits: String = "KEY_SaveLimits"
    val KEY_LIMITS: String = "KEY_LIMITS"
    val KEY_FromNodeInfo: String = "KEY_FromNodeInfo"
    val ImageIDListOffline: List<String> = listOf(
        "motherboard_offline",
        "processor_offline",
        "sytembox_offline",
        "desktop_offline",
        "disk_offline",
        "videocard_offline",
        "laptop_offline"
    )
    val ImageIDListOnline: List<String> = listOf(
        "motherboard_online",
        "processor_online",
        "sytembox_online",
        "desktop_online",
        "disk_online",
        "videocard_online",
        "laptop_online"
    )
    val SAVE_FILE: String = "miner2android.m2a"
    val SAVE_FILE_limits: String = "miner2android_limits.m2a"

    val namesofparams: Map<String, Int> = mapOf(
        "hashrate" to R.string.param_hashrate,
        "hashrate_hour" to R.string.param_hashrate_hour,
        "hashrate_minute" to R.string.param_hashrate_minute,
        "hashrate2" to R.string.param_hashrate2,
        "hashrate_hour2" to R.string.param_hashrate_hour2,
        "hashrate_minute2" to R.string.param_hashrate_minute2,
        "name" to R.string.param_name,
        "vendor" to R.string.param_vendor,
        "power" to R.string.param_power,
        "fan_speed" to R.string.param_fan_speed,
        "temperature" to R.string.param_temperature_gpu,
        "efficiency" to R.string.param_efficiency,
        "efficiency2" to R.string.param_efficiency2,
        "shares" to R.string.param_shares,
        "used_ram" to R.string.param_used_ram,
        "cpu_temp" to R.string.param_cpu_temp,
        "cpu_freq" to R.string.param_cpu_freq,
        "cpu_fan" to R.string.param_cpu_fan,
        )
    val iconsofparams: Map<String, Int> = mapOf(
        "hashrate" to R.drawable.hashrate,
        "hashrate_hour" to R.drawable.hashrate,
        "hashrate_minute" to R.drawable.hashrate,
        "hashrate2" to R.drawable.hashrate,
        "hashrate_hour2" to R.drawable.hashrate,
        "hashrate_minute2" to R.drawable.hashrate,
        "name" to R.drawable.hashrate,
        "vendor" to R.drawable.hashrate,
        "power" to R.drawable.power,
        "fan_speed" to R.drawable.fan,
        "temperature" to R.drawable.temperature,
        "efficiency" to R.drawable.efficiency,
        "efficiency2" to R.drawable.efficiency,
        "shares" to R.drawable.share,
        "used_ram" to R.drawable.used_ram,
        "cpu_temp" to R.drawable.temperature,
        "cpu_freq" to R.drawable.cpu_freq,
        "cpu_fan" to R.drawable.fan,
    )
    val not_hidden_params_gpu: Array<String> = arrayOf(
        "hashrate", "hashrate2", "temperature", "power", "fan_speed"
    )
    val sortKeys: Array<String> = arrayOf(
        "hashrate", "hashrate2", "temperature", "power",
        "fan_speed", "hashrate_minute", "hashrate_minute2",
        "hashrate_hour", "hashrate_hour2"
    )
}