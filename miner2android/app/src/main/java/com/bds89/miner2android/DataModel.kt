package com.bds89.miner2android

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class DataModel : ViewModel() {
    val limits: MutableLiveData<HashMap<String, MutableList<Int>>> by lazy {
        MutableLiveData<HashMap<String, MutableList<Int>>>()
    }
    val PCList: MutableLiveData<ArrayList<PC>> by lazy {
        MutableLiveData<ArrayList<PC>>()
    }
    val responce: MutableLiveData<MutableMap<Int, String>> by lazy {
        MutableLiveData<MutableMap<Int, String>>()
    }
}