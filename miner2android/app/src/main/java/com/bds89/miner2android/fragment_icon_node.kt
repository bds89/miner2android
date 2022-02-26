package com.bds89.miner2android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bds89.miner2android.databinding.FragmentIconNodeBinding
import com.bds89.miner2android.databinding.FragmentNodeInfoBinding

class fragment_icon_node : Fragment() {

    private lateinit var binding: FragmentIconNodeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentIconNodeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val apply = arguments?.takeIf { it.containsKey(const.KEY_IconNum) }?.apply {
            val icon_num = getInt(const.KEY_IconNum)
            binding.ivItemIcon.setImageResource(resources.getIdentifier(const.ImageIDListOnline[icon_num], "drawable", requireActivity().packageName))
        }
    }
}