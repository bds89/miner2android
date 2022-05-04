package com.bds89.miner2android

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.ColorStateList
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.marginLeft
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

    val roundWithSimvol = mapOf<Float, ArrayList<Any>>(
        10f.pow(12) to arrayListOf(10f.pow(9), " B"),
        10f.pow(9) to arrayListOf(10f.pow(6), " M"),
        10f.pow(6) to arrayListOf(10f.pow(3), " K"),
        10f.pow(3) to arrayListOf(10f.pow(0), " "),
    )

    private fun myround(value:Any?):String {
        val vdouble = value.toString().toDoubleOrNull() ?: return "-"
        var output = ""
        run breaking@{
            roundWithSimvol.forEach {
                if (vdouble > it.key) {
                    output = (round ((vdouble / it.value[0] as Float)*10)/10).toString() + it.value[1]
                    return@breaking
                }
            }
        }
        return output
    }

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

                tvMarketcup.text = myround(curList[position].market_cap) + "$"

                //hiden parameters
                var firstClick = true
                llMain.setOnClickListener {

                    if (cvHideble.visibility == View.GONE) {
                        tvCmc.text = curList[position].cmc_rank.toString()

                        tvTags.text = curList[position].tags

                        tvVolume2.text = myround(curList[position].volume_24h) + "$"

                        val chVolume = (round(curList[position].volume_change_24h*100)/100)
                        tvVolume3.text = chVolume.toString() + "%"
                        if (chVolume > 0.2) tvVolume3.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.curUp)))
                        if (chVolume < -0.2) tvVolume3.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.curDown)))

                        tvCirculating.text = myround(curList[position].circulating_supply)
                        tvTotal.text = myround(curList[position].total_supply)
                        tvMax.text = myround(curList[position].max_supply)


                        cvHideble.visibility = View.VISIBLE
                        cvHideble.scaleY = 0f
                        if (!firstClick) cvHideble.x = (0+cvHideble.marginLeft).toFloat()
                        firstClick = false
                        cvHideble.animate().scaleY(1f).setDuration(300).start()

                    }
                    else {
                        cvHideble.animate()
                            .x(cvHideble.width.toFloat())
                            .setDuration(200)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    super.onAnimationEnd(animation)
                                    if (cvHideble.x == cvHideble.width.toFloat()) cvHideble.visibility = View.GONE
                                }
                            })

                    }
                }
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