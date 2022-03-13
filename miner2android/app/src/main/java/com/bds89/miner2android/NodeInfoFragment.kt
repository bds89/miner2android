package com.bds89.miner2android

import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.bds89.miner2android.databinding.DialogVideocardBinding
import com.bds89.miner2android.databinding.FragmentNodeInfoBinding
import com.bds89.miner2android.databinding.SysteemcardBinding
import com.bds89.miner2android.databinding.VideocardBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.round
import kotlin.properties.Delegates


private val client = OkHttpClient()
suspend fun getDataFromServer(pc: PC, req:String, params: MutableMap<String, String> = mutableMapOf()): String =
    withContext(Dispatchers.IO) {
        var ip = pc.ex_IP
        var port = pc.port
        if (pc.ex_IP.isEmpty()) {
            ip = pc.in_IP
            port = pc.in_port
        }

        val url = "http://$ip:${port}/$req"
        //create json of pc object
        val gson: Gson = GsonBuilder().create()
        var data = mutableMapOf(
            "ex_IP" to pc.ex_IP,
            "id" to pc.id,
            "in_IP" to pc.in_IP,
            "in_port" to pc.in_port,
            "name" to pc.name,
            "port" to pc.port,
            "upass" to pc.upass)
        data.putAll(params)
        val jsondata = gson.toJson(data)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(jsondata.toRequestBody(mediaType))
            .build()

        val resp = client.newCall(request).execute()

        return@withContext resp.body!!.string()
    }

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class NodeInfoFragment : Fragment() {
    private lateinit var binding: FragmentNodeInfoBinding
    private lateinit var PCList: ArrayList<PC>
    private lateinit var info: JSONObject
    private lateinit var gpus: JSONArray
    private lateinit var sys_params: JSONObject

    private lateinit var hidden_params: MutableList<Int>
    private lateinit var all_cards_ids: MutableList<Int>
    private lateinit var dialog_videocard: NodeInfoFragment.MyDialogFragment
    private lateinit var limits:HashMap<String, MutableList<Int>>

    private val dataModel:DataModel by activityViewModels()

    private var position by Delegates.notNull<Int>()

    private var responce = ""
    private var responce_from_ViewModel = mutableMapOf<Int, String>()

    private var gpus_lenght:Int = 0
//    private var resonce_time = 0L


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNodeInfoBinding.inflate(inflater)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        resonce_time = System.currentTimeMillis()
        arguments?.takeIf { it.containsKey(const.KEY_PCList) }?.apply {
            position = getInt(const.KEY_PosNum)
            PCList = getSerializable(const.KEY_PCList) as ArrayList<PC>
            limits = getSerializable(const.KEY_LIMITS) as HashMap<String, MutableList<Int>>

        binding.ivItemIcon.setImageResource(
            resources.getIdentifier(
                const.ImageIDListOnline[PCList[position].imageID],
                "drawable",
                requireContext().packageName
            )
        )
        }
        init()
        inflate_cards()

        //observers for save limits, and PCLists, and responce
        dataModel.PCList.observe(activity as LifecycleOwner) {
            PCList = it
        }
        dataModel.limits.observe(activity as LifecycleOwner) {
            limits = it
            GlobalScope.launch(Dispatchers.IO) { send_limits() }
        }
        dataModel.responce.observe(activity as LifecycleOwner) {
            responce_from_ViewModel = it
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.abarHide -> {
                if (PCList[position].visibility) {
                    if (this::hidden_params.isInitialized) {
                        for (id in hidden_params) requireActivity().findViewById<LinearLayout>(id).visibility = View.GONE
                    }
                    PCList[position].visibility = false
                    item.setIcon(R.drawable.ic_baseline_visibility_off_24)
                }
                else {
                    if (this::hidden_params.isInitialized) {
                        for (id in hidden_params) for (id in hidden_params) {
                            requireActivity().findViewById<LinearLayout>(id).visibility = View.VISIBLE
                        }
                    }
                    PCList[position].visibility = true
                    item.setIcon(R.drawable.ic_baseline_visibility_24)
                }
                dataModel.PCList.value = PCList
            }
            R.id.abarRefresh -> {
                inflate_cards(true)
            }
        }
        return true
    }

    private fun inflate_cards(clear:Boolean=false) {
        GlobalScope.launch(Dispatchers.Main) {
            binding.pbMain.visibility = View.VISIBLE
//clear old cards
            if (clear && this@NodeInfoFragment::all_cards_ids.isInitialized) {
                for (id in all_cards_ids) {
                    val view_for_dell: View = requireView().findViewById(id)
                    val parent = view_for_dell.parent as ViewGroup
                    parent.removeView(view_for_dell)
                }
                all_cards_ids = mutableListOf()
            } else all_cards_ids = mutableListOf()
            if (responce_from_ViewModel.isNullOrEmpty() ||
                !responce_from_ViewModel.containsKey(position) ||
                (responce_from_ViewModel.containsKey(position) && responce_from_ViewModel.get(position).isNullOrEmpty())) {
                //get data from server
                try {
                    responce = getDataFromServer(PCList[position], "refresh")
                    info = JSONObject(responce)
                } catch (e: Exception) {
                    val info = JSONObject()
                    info.put("code", 0)
                    info.put("text", e.toString())
                    addcarderror(info)
                }
            } else {
               // get data from ViewModel
                responce = responce_from_ViewModel.get(position).toString()
                responce_from_ViewModel.remove(position)
                dataModel.responce.value = responce_from_ViewModel
                info = JSONObject(responce)
            }
            if (this@NodeInfoFragment::info.isInitialized) {
                //status code
                if (info["code"] == 200) {
//                    //wait complete viewpager animation
//                    val sleep_time = (System.currentTimeMillis() - resonce_time)
//                    if (sleep_time < 350L) Thread.sleep((350L-sleep_time))
                    //GPU parameters
                    try {
                        gpus = info.getJSONObject("data").getJSONArray("gpus")
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.no_gpu_info),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (this@NodeInfoFragment::gpus.isInitialized && gpus.length() > 0) {
                        gpus_lenght = gpus.length()
                        gpus = sortMap(const.sortKeys, gpus)
                        addvideocard(gpus)
                        //Header nodeinfo
                        with(binding) {
                            try {
                                tvGpuTotal.text = info.getJSONObject("data")["gpu_total"].toString()
                                val hash = info.getJSONObject("data")["hashrate"].toString().toDoubleOrNull()
                                if (hash != null) tvNodeHash.text = (round((hash / 10000)) / 100).toString()
                            } catch (e: Exception) {
                                tvGpuTotal.text = ""
                                tvNodeHash.text = ""
                            }
                        }
                    }
                    //System parameters
                    try {
                        sys_params = info.getJSONObject("data").getJSONObject("sys_params")
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.no_sys_info),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (this@NodeInfoFragment::sys_params.isInitialized) addcardsystem(sys_params)
                } else addcarderror(info)
                binding.pbMain.visibility = View.GONE
            }
        }
    }

    //dialog error
    private fun addcarderror(info:JSONObject){
        class MyDialogFragment(
            val title:String,
            val message: String,
            val buttonText:String
        ) : DialogFragment() {

            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                return activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.setTitle(title)
                        .setMessage(message)
                        .setIcon(resources.getIdentifier(
                            const.ImageIDListOnline[PCList[position].imageID],
                            "drawable",
                            requireContext().packageName
                        ))
                        .setPositiveButton(buttonText) {
                                dialog, id ->
                            dialog.cancel()
                            requireActivity().finish()
                        }
                    builder.create()
                } ?: throw IllegalStateException("Activity cannot be null")
            }
        }

        val myDialogFragment = MyDialogFragment("Error: ${info["code"].toString()}", info["text"].toString(), "OK")
        val manager = requireActivity().supportFragmentManager
        myDialogFragment.show(manager, "myDialog")

    }

    private fun addvideocard(gpus: JSONArray) {
        hidden_params = mutableListOf<Int>()
//        gpus.put(gpus.getJSONObject(0))
        for (gpuNum in 0 until gpus.length()) {
            val gpu = gpus.getJSONObject(gpuNum) as JSONObject
            val videocardbinding = VideocardBinding.inflate(layoutInflater)
            val keys: JSONArray = gpu.names()
            for (i in 0 until keys.length()) {
                val key = keys[i] as String
                var value = gpu[key]

                if (key == "name") {
                    videocardbinding.tvNameVideocard.text = value.toString()
                    continue
                }
                if (key == "vendor") {
                    videocardbinding.tvVendorVideocard.text = value.toString()
                    continue
                }

                val tv_for_syscard = LayoutInflater.from(requireContext()).inflate(R.layout.tv_for_syscard, null, false)

                val ll_for_sys_card = tv_for_syscard.findViewById(R.id.ll_for_sys_card) as ConstraintLayout
                val ll_for_card_vert = tv_for_syscard.findViewById(R.id.ll_for_card_vert) as LinearLayout
                ll_for_card_vert.id = View.generateViewId()
                val tv_properti = tv_for_syscard.findViewById(R.id.tv_properti) as TextView
                val tv_value = tv_for_syscard.findViewById(R.id.tv_value) as TextView
                val iv_icon = tv_for_syscard.findViewById(R.id.iv_icon) as ImageView
                val ll_sb = tv_for_syscard.findViewById(R.id.ll_sb) as LinearLayout
                val s_bar = tv_for_syscard.findViewById(R.id.sb) as SeekBar
                val b_below = tv_for_syscard.findViewById(R.id.b_below) as Button
                val b_above = tv_for_syscard.findViewById(R.id.b_above) as Button
                const.iconsofparams[key]?.let { iv_icon.setImageResource(it) }
                if (const.namesofparams.containsKey(key)) tv_properti.text = const.namesofparams[key]?.let { getString(it) }
                else tv_properti.text = key

                //round values and tune sbar
                var double = (value.toString()).toDoubleOrNull()
                if (double != null) {
                    if (double > 1000000) value = round((double)/10000)/100
                    else value = round((double)*100 )/100
                    //click
                    tv_value.setOnClickListener {
                        clickonValue(ll_for_sys_card = ll_for_sys_card, ll_sb, s_bar, tv_value, value, gpuNum, key, b_below, b_above)
                        //tune seekbar
                        tuneSeekBar(s_bar, value, gpuNum, key, b_below, b_above)
                    }
                    //Background chose
                    set_value_bg(ll_for_sys_card = ll_for_sys_card, value = value, gpuNum = gpuNum, key = key)
                }
                tv_value.text = value.toString()

                if (!const.not_hidden_params_gpu.contains(key)) {
                    if (!PCList[position].visibility) ll_for_card_vert.visibility = View.GONE
                    hidden_params.add(ll_for_card_vert.id)
                }
                videocardbinding.llMain.addView(ll_for_sys_card)
            }

            val cardView = videocardbinding.videocard
            if (cardView.getParent() != null) {
                (cardView.getParent() as ViewGroup).removeView(cardView)
            }
            //dialog videocard
            dialog_videocard = MyDialogFragment(PCList[position], gpu, gpuNum)
            val manager = requireActivity().supportFragmentManager
            videocardbinding.imageView.setOnClickListener {
                dialog_videocard.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog_videocard.show(manager, "videocardDialog") }

            cardView.id = View.generateViewId()
            all_cards_ids.add(cardView.id)
            binding.linerMain.addView(cardView)
        }
    }


    private fun addcardsystem(sys_params: JSONObject) {
        //add cardview
        val syscardbinding = SysteemcardBinding.inflate(layoutInflater)
        val keys: JSONArray = sys_params.names()
        for (i in 0 until keys.length()) {
            val key = keys[i] as String
            var value = sys_params[key]

            val tv_for_syscard = LayoutInflater.from(requireContext()).inflate(R.layout.tv_for_syscard, null, false)
            val ll_for_sys_card = tv_for_syscard.findViewById(R.id.ll_for_sys_card) as ConstraintLayout
            val tv_properti = tv_for_syscard.findViewById(R.id.tv_properti) as TextView
            val tv_value = tv_for_syscard.findViewById(R.id.tv_value) as TextView
            val iv_icon = tv_for_syscard.findViewById(R.id.iv_icon) as ImageView
            val ll_sb = tv_for_syscard.findViewById(R.id.ll_sb) as LinearLayout
            val s_bar = tv_for_syscard.findViewById(R.id.sb) as SeekBar
            val b_below = tv_for_syscard.findViewById(R.id.b_below) as Button
            val b_above = tv_for_syscard.findViewById(R.id.b_above) as Button
            const.iconsofparams[key]?.let { iv_icon.setImageResource(it) }
            if (const.namesofparams.containsKey(key)) tv_properti.text = const.namesofparams[key]?.let { getString(it) }
            else tv_properti.text = key

            //round values and tune sbar
            var double = (value.toString()).toDoubleOrNull()
            if (double != null) {
                if (double > 1000000) value = round((double)/10000)/100
                else value = round((double)*100 )/100
                //click
                tv_value.setOnClickListener {clickonValue(ll_for_sys_card = ll_for_sys_card, ll_sb = ll_sb, s_bar = s_bar, tv_value = tv_value, value = value, key = key, b_below = b_below, b_above = b_above)}
                //tune seekbar
                tuneSeekBar(s_bar = s_bar, value = value, key = key, b_below = b_below, b_above = b_above)
                //Background chose
                set_value_bg(ll_for_sys_card = ll_for_sys_card, value = value, key = key)
            }

            tv_value.text = value.toString()
            syscardbinding.llMain.addView(ll_for_sys_card)
        }


        val cardView = syscardbinding.systemcard
        if (cardView.getParent() != null) {
            (cardView.getParent() as ViewGroup).removeView(cardView)
        }
        cardView.id = View.generateViewId()
        all_cards_ids.add(cardView.id)
        binding.linerMain.addView(cardView)
    }
    private fun init() {
        binding.apply {
            swipeContainer.setOnRefreshListener {
                inflate_cards(true)
                swipeContainer.isRefreshing = false
            }
        }
    }

    private fun clickonValue(ll_for_sys_card:ConstraintLayout, ll_sb: LinearLayout, s_bar:SeekBar, tv_value:TextView, value:Double, gpuNum:Int=99999, key:String, b_below:Button, b_above:Button) {
        if (ll_sb.visibility == View.GONE) {
            //on unhide seekBar
            ll_sb.visibility = View.VISIBLE
            tv_value.text = s_bar.progress.toString()
            tv_value.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.teal_700)))
            s_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    tv_value.text = progress.toString()
                    set_value_bg(ll_for_sys_card = ll_for_sys_card, value = value, gpuNum = gpuNum, key = key, progress = progress)

                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {    }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {    }
            })
        }
        else {
            //on hide seekBar
            if (limits.containsKey("${PCList[position].name}_${gpuNum}_$key")) {
                limits["${PCList[position].name}_${gpuNum}_$key"]?.set(0, s_bar.progress)
            }
            else {
                limits.put("${PCList[position].name}_${gpuNum}_$key", mutableListOf(s_bar.progress, 1))
            }
            ll_sb.visibility = View.GONE
            tv_value.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text)))
            tv_value.text = value.toString()
            dataModel.limits.value = limits
        }
        b_below.setOnClickListener {
            if (limits.containsKey("${PCList[position].name}_${gpuNum}_$key")) {
                limits["${PCList[position].name}_${gpuNum}_$key"]?.set(0, s_bar.progress)
                limits["${PCList[position].name}_${gpuNum}_$key"]?.set(1, 0)
            }
            else {
                limits.put("${PCList[position].name}_${gpuNum}_$key", mutableListOf(s_bar.progress, 0))
            }
            b_above.backgroundTintList = (ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text)))
            b_below.backgroundTintList = (ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.teal_700)))
            GlobalScope.launch(Dispatchers.Main) {
                delay(200L)
                ll_sb.visibility = View.GONE
                tv_value.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text)))
                tv_value.text = value.toString()
            }
            set_value_bg(ll_for_sys_card = ll_for_sys_card, value = value, gpuNum = gpuNum, key = key)
            dataModel.limits.value = limits
        }
        b_above.setOnClickListener {
            if (limits.containsKey("${PCList[position].name}_${gpuNum}_$key")) {
                limits["${PCList[position].name}_${gpuNum}_$key"]?.set(0, s_bar.progress)
                limits["${PCList[position].name}_${gpuNum}_$key"]?.set(1, 1)
            }
            else {
                limits.put("${PCList[position].name}_${gpuNum}_$key", mutableListOf(s_bar.progress, 1))
            }

            b_below.backgroundTintList = (ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text)))
            b_above.backgroundTintList = (ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.teal_700)))
            GlobalScope.launch(Dispatchers.Main) {
                delay(200L)
                ll_sb.visibility = View.GONE
                tv_value.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text)))
                tv_value.text = value.toString()
            }
            set_value_bg(ll_for_sys_card = ll_for_sys_card, value = value, gpuNum = gpuNum, key = key)
            dataModel.limits.value = limits
        }
    }

    private fun tuneSeekBar(s_bar: SeekBar, value: Double, gpuNum:Int=99999, key:String, b_below:Button, b_above:Button) {
        s_bar.max = value.toInt()*2
        if ((limits.containsKey("${PCList[position].name}_${gpuNum}_$key")) &&
                (limits["${PCList[position].name}_${gpuNum}_$key"] != null)) {
                    s_bar.progress = limits["${PCList[position].name}_${gpuNum}_$key"]?.get(0)!!
                    if (limits["${PCList[position].name}_${gpuNum}_$key"]?.get(1)!! == 0) b_below.backgroundTintList = (ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.teal_700)))
            else b_above.backgroundTintList = (ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.teal_700)))
        }
        else s_bar.progress = s_bar.max
        }

    private fun set_value_bg(ll_for_sys_card:ConstraintLayout, value:Double, gpuNum:Int=99999, key:String, progress: Int? =null) {

        val animationDrawable: AnimationDrawable = ll_for_sys_card.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(1000)
        animationDrawable.setExitFadeDuration(1000)
        animationDrawable.isOneShot = true

        if (progress != null) {
            if ((limits.containsKey("${PCList[position].name}_${gpuNum}_$key")) && (limits["${PCList[position].name}_${gpuNum}_$key"] != null)) {
                val limType = limits["${PCList[position].name}_${gpuNum}_$key"]?.get(1)
                    if (limType == 0) {
                        if (value < progress) animationDrawable.start()
                        else ll_for_sys_card.background = ContextCompat.getDrawable(requireContext(), R.drawable.warning_bg)
                    } else {
                        if (value > progress) animationDrawable.start()
                        else ll_for_sys_card.background = ContextCompat.getDrawable(requireContext(), R.drawable.warning_bg)
                    }
                }
        } else {
            if ((limits.containsKey("${PCList[position].name}_${gpuNum}_$key")) && (limits["${PCList[position].name}_${gpuNum}_$key"] != null)) {
                val lim = limits["${PCList[position].name}_${gpuNum}_$key"]?.get(0)
                val limType = limits["${PCList[position].name}_${gpuNum}_$key"]?.get(1)
                if (limType == 0) {
                    if (value < lim!!) animationDrawable.start()
                    else ll_for_sys_card.background = ContextCompat.getDrawable(requireContext(), R.drawable.warning_bg)
                } else {
                    if (value > lim!!) animationDrawable.start()
                    else ll_for_sys_card.background = ContextCompat.getDrawable(requireContext(), R.drawable.warning_bg)
                }
            }
        }
    }
    private fun sortMap(sortKeys:Array<String>, forSort: JSONArray): JSONArray {
        Log.e("ml", forSort.toString())
        var sorted = JSONArray()
        for (i in 0 until forSort.length()) {
            var gpu = forSort.getJSONObject(i) as JSONObject
            var sorted_obj = JSONObject()
            for (key in sortKeys) {
                if (gpu.has(key)) {
                    sorted_obj.put(key, gpu.get(key))
                    gpu.remove(key)
                }
            }
            Log.e("ml", gpu.toString())
            if (gpu.length() > 0) {
                val keys: JSONArray = gpu.names()
                for (i in 0 until keys.length()) {
                    sorted_obj.put(keys.getString(i), gpu.get(keys.getString(i)))
                }
            }
            sorted.put(sorted_obj)
        }
        return sorted
    }
//dialog videocard
class MyDialogFragment(val PC: PC,
                       val gpu:JSONObject,
                        val GPUNum: Int) : DialogFragment() {

    private var responce = ""
    private lateinit var info:JSONObject

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val binding_d = DialogVideocardBinding.inflate(layoutInflater)
        with(binding_d) {
            //titul
            tvCardName.text = gpu.get("name").toString()
            tvCarVendor.text = gpu.get("vendor").toString()
            //power limit
            var pw: Int? = null
            pw = gpu.get("power").toString().toDoubleOrNull()?.toInt()
            if (pw != null) {
                pw = pw.toInt()
                sbPowerlimit.max = pw * 2
                sbPowerlimit.progress = pw
                tvPowerLimit.text = pw.toString()
            }
            sbPowerlimit.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    tvPowerLimit.text = progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            //fan
            var fan: Int? = null
            fan = gpu.get("fan_speed").toString().toDoubleOrNull()?.toInt()
            if (fan != null) {
                sbFan.max = 100
                sbFan.progress = fan
                tvFan.text = fan.toString()
            }
            tvLog.setMovementMethod(ScrollingMovementMethod())
//            //fan mode
            GlobalScope.launch(Dispatchers.Main) {
                //get data from server
                val params = mutableMapOf("card" to GPUNum.toString())
                try {
                    responce = getDataFromServer(PC, "get_fan_mode", params)
                    Log.e("ml", "Responce: $responce")
                    info = JSONObject(responce)
                } catch (e: Exception) {
                    Log.e("ml", "error")
                    tvLog.text = e.toString()
                    tvLog.visibility = View.VISIBLE
                }
                if (this@MyDialogFragment::info.isInitialized) {
                    //status code
                    if (info["code"] == 200) {
                        val fan_mode = (info.getJSONObject("data") as JSONObject).get("fan_mode")
                        swFanMode.isChecked = fan_mode == "auto"
                    } else tvLog.text = "code: ${info["code"]}. ${info["text"]}"
                } else swFanMode.visibility = View.GONE
            }
                sbFan.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        tvFan.text = progress.toString()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
                //apply button
                bApply.setOnClickListener {
                    GlobalScope.launch(Dispatchers.Main) {
                        tvLog.visibility = View.VISIBLE
                        if (pw != sbPowerlimit.progress) {
                            val change_succes = gpuControl(GPUNum, "power_limit", sbPowerlimit.progress.toString(), PC, binding_d)
                            if (change_succes) pw = sbPowerlimit.progress
                        }
                        if (fan != sbFan.progress) {
                            val change_succes = gpuControl(GPUNum, "fan_speed", sbFan.progress.toString(), PC, binding_d)
                            if (change_succes) fan = sbFan.progress
                        }
                    }
                }
            //switch fan mode
            swFanMode.setOnClickListener {
                GlobalScope.launch(Dispatchers.Main) {
                    if (swFanMode.isChecked) gpuControl(GPUNum, "fan_mode", "auto", PC, binding_d)
                    else gpuControl(GPUNum, "fan_mode", "manual", PC, binding_d)
                }
            }

            builder.setView(binding_d.root)
            return builder.create()
        }
    }
    suspend fun gpuControl(GPUNum: Int, request: String, value: String, PC:PC, binding_d: DialogVideocardBinding):Boolean {
        val params = mutableMapOf("value" to value, "card" to GPUNum.toString(), "request" to request)
        var answer = "no response"
        var responce_ = ""
        var info_: JSONObject
        try {
            responce_ = getDataFromServer(PC, "control", params)
            info_ = JSONObject(responce_)
            } catch (e: Exception) {
                answer = e.toString()
                binding_d.tvLog.visibility = View.VISIBLE
                binding_d.tvLog.text = answer
                return false
            }
            //status code
            if (info_["code"] == 200) {
                answer = info_["text"].toString()
                if (info_.has("fan_mode") && info_.get("fan_mode") == "manual") binding_d.swFanMode.isChecked = false
                if (info_.has("fan_mode") && info_.get("fan_mode") == "auto") binding_d.swFanMode.isChecked = true
                binding_d.tvLog.visibility = View.VISIBLE
                binding_d.tvLog.text = answer
                return true
            } else answer = "code: ${info_["code"]}. ${info_["text"]}"
        binding_d.tvLog.visibility = View.VISIBLE
        binding_d.tvLog.text = answer
        return false
        }

    override fun onPause() {
        super.onPause()
        dismiss()
    }
    }

    suspend fun send_limits(){
        val pc = PCList[position]
        val new_limits_one_rig = hashMapOf<String, MutableMap<String, List<Int>>>()
        limits.forEach { (key, value) ->
            for (gpuNum in 0 until gpus_lenght) {
                if ("${pc.name}_${gpuNum}_" in key) {
                    val new_key = key.drop("${pc.name}_${gpuNum}_".length)
                        if (new_limits_one_rig.containsKey("$gpuNum")) new_limits_one_rig["$gpuNum"]?.put(new_key, value)
                    else new_limits_one_rig.put("$gpuNum", mutableMapOf(new_key to value))
                }
            }
            if ("${pc.name}_99999_" in key) {
                val new_key = key.drop("${pc.name}_${99999}_".length)
                if (new_limits_one_rig.containsKey("99999")) new_limits_one_rig["99999"]?.put(new_key, value)
                else new_limits_one_rig.put("99999", mutableMapOf(new_key to value))
            }
        }
        var ip = pc.ex_IP
        if (pc.ex_IP.isEmpty()) ip = pc.in_IP

        val url = "http://$ip:${pc.port}/control"
        //create json of pc object
        val gson: Gson = GsonBuilder().create()
        var data = mutableMapOf<String, Any>(
        "ex_IP" to pc.ex_IP,
        "id" to pc.id,
        "in_IP" to pc.in_IP,
        "in_port" to pc.in_port,
        "name" to pc.name,
        "port" to pc.port,
        "upass" to pc.upass,
        "request" to "send_limits",
        "value" to new_limits_one_rig)

        val jsondata = gson.toJson(data)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(jsondata.toRequestBody(mediaType))
            .build()

        var inf = JSONObject()
        try {
            val resp = client.newCall(request).execute()
            inf = JSONObject(resp.body!!.string())
        } catch (e: Exception) {
            inf.put("code", 0)
            inf.put("text", e.toString())
        }
        if (inf.get("code") != 200) addcarderror(inf)
    }

    override fun onSaveInstanceState(saveInstanceState: Bundle) {
        responce_from_ViewModel.put(position, responce)
        dataModel.responce.value = responce_from_ViewModel
        super.onSaveInstanceState(saveInstanceState)
    }
}