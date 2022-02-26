package com.bds89.miner2android

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

    class NodeInfoAdapter(fragment: FragmentActivity, val PCList: ArrayList<PC>, val limits: HashMap<String, MutableList<Int>>) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return PCList.size
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = NodeInfoFragment()
        fragment.arguments = Bundle().apply {
            putInt(const.KEY_PosNum, position)
            putSerializable(const.KEY_PCList, PCList)
            putSerializable(const.KEY_LIMITS, limits)
        }
        return fragment
    }

}