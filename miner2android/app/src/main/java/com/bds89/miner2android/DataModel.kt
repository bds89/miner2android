package com.bds89.miner2android

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bds89.miner2android.forRoom.LimitEntity
import org.json.JSONObject

open class DataModel : ViewModel() {
    val limits: MutableLiveData<ArrayList<LimitEntity?>> by lazy {
        MutableLiveData<ArrayList<LimitEntity?>>()
    }
    val PCList: MutableLiveData<ArrayList<PC>> by lazy {
        MutableLiveData<ArrayList<PC>>()
    }
    val responce: MutableLiveData<MutableMap<Int, String>> by lazy {
        MutableLiveData<MutableMap<Int, String>>()
    }
}