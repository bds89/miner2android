package com.bds89.miner2android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bds89.miner2android.databinding.MenuPcItemBinding

class MenuPCadapter(var PCList: ArrayList<PC>,
                    val current_pc_poition: Int,
                    private var itemClickListener: MenuPCadapter.ItemClickListener
): RecyclerView.Adapter<MenuPCadapter.PCHolder>() {

    interface ItemClickListener {
        fun itemClicked(position: Int)
    }
    inner class PCHolder(item: View): RecyclerView.ViewHolder(item) {
        val binding = MenuPcItemBinding.bind(item)
        val context = item.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PCHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.menu_pc_item, parent, false)
        return PCHolder(view)
    }

    override fun onBindViewHolder(holder: PCHolder, position: Int) {
        holder.binding.tvMenuItem.text = PCList[position].name
        if (position == current_pc_poition) holder.binding.cvMenuItem.setBackgroundColor(
            ContextCompat.getColor(holder.context, R.color.teal_700))
        holder.binding.tvMenuItem.setOnClickListener(){
            holder.binding.cvMenuItem.setBackgroundColor(
                ContextCompat.getColor(holder.context, R.color.teal_700))
            itemClickListener.itemClicked(position)
        }
    }

    override fun getItemCount(): Int {
        return PCList.size
    }

    fun refreshList(NewPCList: ArrayList<PC>){
        PCList = NewPCList
        notifyDataSetChanged()
    }
}