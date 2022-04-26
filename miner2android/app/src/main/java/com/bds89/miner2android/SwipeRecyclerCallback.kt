package com.bds89.miner2android

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView


open class SwipeRecyclerCallback(val adapter: BottomSheetAdapter): ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val startPosition = viewHolder.adapterPosition
        val endPosition = target.adapterPosition

        recyclerView.adapter?.notifyItemMoved(startPosition, endPosition)
        // true if moved, false otherwise
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter.dellCur(viewHolder.adapterPosition)
    }
}