package com.bds89.miner2android

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bds89.miner2android.databinding.BottomSheetItemBinding
import kotlin.math.pow
import kotlin.math.round

class BottomSheetAdapter(var curList: ArrayList<CUR>): RecyclerView.Adapter<BottomSheetAdapter.CurHolder>() {

    val roundPriceMap = mapOf<Double, Int>(
        10000.0 to 0,
        1000.0 to 1,
        100.0 to 2,
        10.0 to 3,
        1.0 to 4,
        0.1 to 5,
        0.01 to 6,
        0.001 to 7,
        0.0001 to 8,
    )

    inner class CurHolder(item: View): RecyclerView.ViewHolder(item) {
        val binding = BottomSheetItemBinding.bind(item)
        val context = item.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomSheetAdapter.CurHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bottom_sheet_item, parent, false)
        return CurHolder(view)
    }

    override fun onBindViewHolder(holder: BottomSheetAdapter.CurHolder, position: Int) {
        with(holder){
            with(binding){
                //animation
                root.animation = AnimationUtils.loadAnimation(context, R.anim.bottomrecycler)

                tvName.text = curList[position].name
                tvSymbol.text = curList[position].symbol

                val ch24 = (round(curList[position].percent_change_24h*100)/100)
                tvChange24.text = ch24.toString() + "%"
                if (ch24 > 0.2) tvChange24.background = AppCompatResources.getDrawable(context, R.drawable.cur__price_background_up)
                if (ch24 < -0.2) tvChange24.background = AppCompatResources.getDrawable(context, R.drawable.cur__price_background_down)

                val ch1 = (round(curList[position].percent_change_1h*100)/100)
                tvChange1.text = ch1.toString() + "%"
                if (ch1 > 0.2) tvChange1.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.curUp)))
                if (ch1 < -0.2) tvChange1.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.curDown)))

                run breaking@{
                    roundPriceMap.forEach { (above, i) ->
                        if (curList[position].price > above) {
                            val price = round(curList[position].price * (10f.pow(i))) / 10f.pow(i)
                            tvPrice.text = price.toString()
                            return@breaking
                        }
                    }
                }

                tvMarketcup.text = (round(curList[position].market_cap/1000000)).toString() + " M$"
            }
        }
    }

    override fun getItemCount(): Int {
        return curList.size
    }

    fun refresh(curs:ArrayList<CUR>) {
        curList = curs
        notifyDataSetChanged()
    }
    fun addCur(cur: CUR, needRefresh:Boolean=false) {
        curList.add(cur)
        if (needRefresh) notifyDataSetChanged()
        else notifyItemChanged(curList.indexOf(cur))
    }

    fun dellCur(position: Int) {
//        curList.removeAt(position)
        notifyItemRemoved(position)
    }
}