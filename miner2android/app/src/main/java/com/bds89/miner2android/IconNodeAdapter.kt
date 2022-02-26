package com.bds89.miner2android

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class IconNodeAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return const.ImageIDListOnline.size
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = fragment_icon_node()
        fragment.arguments = Bundle().apply {
            putInt(const.KEY_IconNum, position)
        }
        return fragment
    }

}