package com.bds89.miner2android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bds89.miner2android.databinding.PcItemBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.lang.Exception

class PCadapter(
    var PCList: ArrayList<PC>,
    private var optionsMenuClickListener: OptionsMenuClickListener,
    private var itemClickListener: ItemClickListener
    ): RecyclerView.Adapter<PCadapter.PCHolder>() {

    val client = OkHttpClient()
    var refreshing:MutableMap<Int, Boolean> = mutableMapOf()
    interface OptionsMenuClickListener {
        fun onOptionsMenuClicked(position: Int): Boolean
    }

    interface ItemClickListener {
        fun itemClicked(position: Int)
    }

    inner class PCHolder(item: View): RecyclerView.ViewHolder(item) {
            val binding = PcItemBinding.bind(item)
            val context = item.context
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PCHolder {

            val view = LayoutInflater.from(parent.context).inflate(R.layout.pc_item, parent, false)
        return PCHolder(view)
    }

    override fun onBindViewHolder(holder: PCHolder, position: Int) {
        with(holder){
            with(binding){
                if (refreshing.containsKey(position) && refreshing[position] == true) {
                    tvStatus.visibility = View.GONE
                    pbPing.visibility = View.VISIBLE
                }
                else {
                    tvStatus.visibility = View.VISIBLE
                    pbPing.visibility = View.GONE
                }

                if (PCList[position].status != "offline") {
                        ivPc.setImageResource(
                            holder.context.resources.getIdentifier(
                                const.ImageIDListOnline[PCList[position].imageID],
                                "drawable",
                                holder.context.packageName
                            )
                        )
                    } else {
                        ivPc.setImageResource(
                            holder.context.resources.getIdentifier(
                                const.ImageIDListOffline[PCList[position].imageID],
                                "drawable",
                                holder.context.packageName
                            )
                        )
                    }
                tvTitle.text = PCList[position].name
                tvStatus.text = PCList[position].status
                ivPc.setOnClickListener(){
                    itemClickListener.itemClicked(position)
                }
                tvMenu.setOnClickListener {
                    optionsMenuClickListener.onOptionsMenuClicked(position)
                }
                ivPc.setOnLongClickListener {
                    optionsMenuClickListener.onOptionsMenuClicked(position)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return PCList.size
    }

    fun addPC(pc: PC) {
        var maxID:Int = 0
        for (p in PCList) {
            if (p.id > maxID) maxID = p.id
        }
        pc.id = maxID+1
        var pc_names = mutableListOf<String>()
        PCList.add(pc)
        PCList.sortBy { PC -> PC.name }
        notifyDataSetChanged()
    }

    fun editPC(pc: PC) {
        for (oldpc in PCList) {
            if (oldpc.id == pc.id) {
                PCList[PCList.indexOf(oldpc)] = pc
                break
            }
        }
        notifyDataSetChanged()
    }

    fun delPC(id: Int): Boolean {
        for (pc in PCList) {
            if (pc.id == id) {
                PCList.removeAt(PCList.indexOf(pc))
                notifyDataSetChanged()
                return true
            }
        }
        return false
    }

    fun refreshPCs() {
        PCList.forEachIndexed { index, element ->
            GlobalScope.launch(Dispatchers.Main) {
            if (refreshing.containsKey(index)) refreshing[index] = true
            else refreshing.put(index, true)
            notifyDataSetChanged()

            PCList[index].status = getStatus(element)

            if (refreshing.containsKey(index)) refreshing[index] = false
            else refreshing.put(index, false)
            notifyDataSetChanged()
            }
        }
    }

    suspend fun getStatus(pc: PC): String =
        withContext(Dispatchers.IO) {
            var ip = pc.ex_IP
            var port = pc.port
            if (pc.ex_IP.isEmpty()) {
                ip = pc.in_IP
                port = pc.in_port
            }

            if (port.toIntOrNull() == null) return@withContext "error url"
            val url = "http://$ip:$port/ping"
            //create json of pc object
            val gson: Gson = GsonBuilder().create()
            val jsondata = gson.toJson(pc)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(jsondata.toRequestBody(mediaType))
                    .build()


                val t1 = System.currentTimeMillis()
                val responce = client.newCall(request).execute().body!!.string()
                val info = JSONObject(responce)
                val time_answer = System.currentTimeMillis() - t1
                if (info["code"] == 200) {
                    return@withContext "$time_answer ms"
                }
            } catch (e: Exception) {
            }
            return@withContext "offline"
        }
}