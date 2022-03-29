package com.bds89.miner2android

import android.graphics.drawable.AnimationDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
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

import android.graphics.drawable.Drawable
import android.view.animation.AnimationUtils


class PCadapter(
    var PCList: ArrayList<PC>,
    private var optionsMenuClickListener: OptionsMenuClickListener,
    private var itemClickListener: ItemClickListener
    ): RecyclerView.Adapter<PCadapter.PCHolder>() {

    val client = OkHttpClient()
    var refreshing:MutableMap<Int, Boolean> = mutableMapOf()
    var overload_limits_names = arrayListOf<String>()

    interface OptionsMenuClickListener {
        fun onOptionsMenuClicked(position: Int, ivPc: ImageView): Boolean
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
                    root.animation =
                        AnimationUtils.loadAnimation(context, R.anim.pcrecycler)
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
                //if limits, change background
                if (overload_limits_names.contains(PCList[position].name)) {
                    val animationDrawable: AnimationDrawable = llPcitem.background as AnimationDrawable
                    animationDrawable.setEnterFadeDuration(500)
                    animationDrawable.setExitFadeDuration(1000)
                    animationDrawable.isOneShot = true
                    animationDrawable.start()
                }

                tvTitle.text = PCList[position].name
                tvStatus.text = PCList[position].status
                ivPc.setOnClickListener(){
                    itemClickListener.itemClicked(position)
                }
                tvMenu.setOnClickListener {
                    optionsMenuClickListener.onOptionsMenuClicked(position, ivPc)


                }
                ivPc.setOnLongClickListener {
                    optionsMenuClickListener.onOptionsMenuClicked(position, ivPc)
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
        //check limits
        overload_limits_names.clear()
        var ips = hashMapOf<String, MutableMap<String, String>>()
        val jobGetOverload = GlobalScope.launch(Dispatchers.IO) {
            if (!PCList.isNullOrEmpty()) {
                PCList.forEachIndexed { index, PC ->

                    //show progress bar
                    withContext(Dispatchers.Main) {
                        if (refreshing.containsKey(index)) refreshing[index] = true
                        else refreshing.put(index, true)
                        notifyItemChanged(index)
                    }


                    if (PC.in_IP == "") {
                        if (!ips.containsKey("${PC.ex_IP}:${PC.port}")) ips.put(
                            "${PC.ex_IP}:${PC.port}",
                            mutableMapOf()
                        )
                    } else if (PC.ex_IP == "") {
                        if (!ips.containsKey("${PC.in_IP}:${PC.in_port}")) ips.put(
                            "${PC.in_IP}:${PC.in_port}",
                            mutableMapOf()
                        )
                    } else {
                        if (!ips.containsKey("${PC.ex_IP}:${PC.port}")) ips.put(
                            "${PC.ex_IP}:${PC.port}",
                            mutableMapOf(PC.name to "${PC.in_IP}:${PC.in_port}")
                        )
                        else ips["${PC.ex_IP}:${PC.port}"]?.put(
                            PC.name,
                            "${PC.in_IP}:${PC.in_port}"
                        )
                    }
                }
                ips.forEach { ip ->
                    val url = "http://${ip.key}/control"
                    PCList.forEach findPC@{ pc ->
                        if ("${pc.ex_IP}:${pc.port}" == ip.key && pc.in_IP == "") {
                            //create json of pc object
                            val gson: Gson = GsonBuilder().create()
                            val data = mutableMapOf(
                                "ex_IP" to pc.ex_IP,
                                "id" to pc.id,
                                "in_IP" to pc.in_IP,
                                "in_port" to pc.in_port,
                                "name" to pc.name,
                                "port" to pc.port,
                                "upass" to pc.upass,
                                "request" to "check_limits",
                                "value" to ip.value,
                                "full_check" to true
                            )
                            val jsondata = gson.toJson(data)
                            val mediaType = "application/json; charset=utf-8".toMediaType()
                            val request = Request.Builder()
                                .url(url)
                                .post(jsondata.toRequestBody(mediaType))
                                .build()

                            try {
                                val resp = client.newCall(request).execute().body!!.string()
                                val jsn = JSONObject(resp)
                                if (jsn.get("code") == 200) {
                                    val jsnData: JSONObject = jsn.get("data") as JSONObject
                                    jsnData.keys().forEach { key ->
                                        if (jsnData.get(key) !is Int) overload_limits_names.add(key)
                                    }
                                }
                            } catch (e: java.lang.Exception) {
                            }
                            return@findPC
                        }
                    }
                }
            }
        }
        //refresh
        PCList.forEachIndexed { index, element ->
            GlobalScope.launch(Dispatchers.Main) {
                jobGetOverload.join()

                PCList[index].status = getStatus(element)

                if (refreshing.containsKey(index)) refreshing[index] = false
                else refreshing.put(index, false)
                notifyItemChanged(index)
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