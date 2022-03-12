package com.bds89.miner2android

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.*
import com.bds89.miner2android.BuildConfig.APPLICATION_ID
import com.bds89.miner2android.databinding.ActivitySettingsBinding
import com.bds89.miner2android.databinding.DialogSaveloadBinding
import com.bds89.miner2android.databinding.DialogThemeBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding:ActivitySettingsBinding
    private lateinit var dialog_theme: Dialog
    private lateinit var dialog_saveLoad: Dialog
    private lateinit var settings: HashMap<String, String>
    private lateinit var myWorkRequest: PeriodicWorkRequest
    private lateinit var getFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var PCList :ArrayList<PC>
    private lateinit var limits:HashMap<String, MutableList<Int>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(com.bds89.miner2android.R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //get or load settings
        if (intent.getSerializableExtra(const.KEY_SaveSettings) == null) {
            //load settings
            val settingLoad = load(const.KEY_SaveSettings)
            if (settingLoad != false) {
                settings = settingLoad as HashMap<String, String>
            } else settings = hashMapOf<String, String>()
            //get settings
        } else settings = intent.getSerializableExtra(const.KEY_SaveSettings) as HashMap<String, String>


        dialog_theme_inflate()
        dialog_saveload_inflate()
        LimitsNotifycationWork()
        init_onclicks()
    }

    override fun onPause() {
        if(dialog_theme != null){
            dialog_theme.dismiss()
        }
        super.onPause()
    }

    private fun dialog_theme_inflate() {
        dialog_theme = Dialog(this)

        // Передайте ссылку на разметку
        val binding_d = DialogThemeBinding.inflate(layoutInflater)
        dialog_theme.setContentView(binding_d.root)

        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                binding.tvTheme.text = getString(R.string.light)
                binding_d.rg.check(binding_d.rbLight.id)
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                binding.tvTheme.text = getString(R.string.dark)
                binding_d.rg.check(binding_d.rbDark.id)
            }
            else -> {
                binding.tvTheme.text = getString(R.string.system)
                binding_d.rg.check(binding_d.rbSystem.id)
            }
        }
        binding_d.rg.setOnCheckedChangeListener { radioGroup, i ->
            when (i) {
                binding_d.rbSystem.id -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                binding_d.rbLight.id -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                binding_d.rbDark.id -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            if (settings.containsKey("theme")) settings["theme"] = AppCompatDelegate.getDefaultNightMode().toString()
            else settings.put("theme", AppCompatDelegate.getDefaultNightMode().toString())
            save(settings, const.KEY_SaveSettings)
            now_theme()
            dialog_theme.hide()
            binding.llTheme.setBackgroundColor(0)
        }
    }

    private fun dialog_saveload_inflate() {
        dialog_saveLoad = Dialog(this)
        val dir: File = filesDir
        // Передайте ссылку на разметку
        val binding_d = DialogSaveloadBinding.inflate(layoutInflater)
        with(binding_d) {
            tvSave.setOnClickListener {
                var all_settings = hashMapOf<String, Any>()
                all_settings.put("settings", load(const.KEY_SaveSettings) as HashMap<String, String>)
                all_settings.put("PCList", load(const.KEY_SavePC) as ArrayList<PC>)
                all_settings.put("limits", load(const.KEY_SaveLimits) as HashMap<String, MutableList<Int>>)
                //create file
                val fileSave = FileOutputStream("$dir/m2a_settings")
                val outStream = ObjectOutputStream(fileSave)
                outStream.writeObject(all_settings)
                outStream.close()
                fileSave.close()
                //create URI
                val file = File("$dir/m2a_settings")
                val uri = FileProvider.getUriForFile(
                    Objects.requireNonNull(applicationContext),
                    APPLICATION_ID + ".provider",
                    file)
                //create intent
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "*/*"
                }
                startActivity(Intent.createChooser(shareIntent, null))
                dialog_saveLoad.cancel()
            }
            tvLoad.setOnClickListener {
                val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)

                getFileLauncher.launch(Intent.createChooser(intent, "Select a file"))
                dialog_saveLoad.cancel()
            }
        }
        dialog_saveLoad.setContentView(binding_d.root)

        //after choose file to load
        getFileLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK && it.data != null) {
                    val uri = it.data?.data
                    if(uri != null) {
                        var FileinputStream = this.contentResolver.openInputStream(uri)
                        val inStream = ObjectInputStream(FileinputStream)
                        val loadedData = inStream.readObject()
                        inStream.close()
                        FileinputStream?.close()

                        try {
                            binding.pbApplySettings.max = 100
                            binding.pbApplySettings.progress = 0
                            binding.pbApplySettings.visibility = View.VISIBLE
                            val all_settings = loadedData as HashMap<Any, Any>
                            //save every data
                            //settings
                            if (all_settings.containsKey("settings") && all_settings["settings"] != false && all_settings["settings"] != null) {
                                save((all_settings["settings"]!! as HashMap<String, String>), const.KEY_SaveSettings)
                                //apply settings
                                val settings = all_settings["settings"] as HashMap<String, String>
                                //theme
                                if (settings.containsKey("theme")) settings["theme"]?.let {
                                    AppCompatDelegate.setDefaultNightMode(
                                        it.toInt()
                                    )
                                }
                            }
                            binding.pbApplySettings.progress = 5
                            //PCList
                            if (all_settings.containsKey("PCList") && all_settings["PCList"] != false && all_settings["PCList"] != null) {
                                save((all_settings["PCList"]!! as ArrayList<PC>), const.KEY_SavePC)
                                PCList = all_settings["PCList"]!! as ArrayList<PC>
                                binding.pbApplySettings.progress = 10
                                //limits
                                if (all_settings.containsKey("limits") && all_settings["limits"] != false && all_settings["limits"] != null) {
                                    limits = all_settings["limits"]!! as HashMap<String, MutableList<Int>>
                                    save(limits, const.KEY_SaveLimits)
                                    val one_pc_progress = (90/PCList.size).toInt()
                                    PCList.forEach { pc->
                                        val job = GlobalScope.launch(Dispatchers.IO) {
                                            if (!send_limits(pc)) Toast.makeText(
                                                this@SettingsActivity,
                                                "Can't load ${pc.name}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        GlobalScope.launch(Dispatchers.Main) {
                                            job.join()
                                            binding.pbApplySettings.progress += one_pc_progress
                                        }
                                    }
                                }
                            }

                        } catch (e: Exception) {
                            Log.e("ml", e.toString())
                            Toast.makeText(this, "Can't load this settings", Toast.LENGTH_LONG).show()
                            return@registerForActivityResult
                        }
                        binding.pbApplySettings.visibility = View.GONE
                    }
                }
            }
    }

    fun now_theme(){
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                binding.tvTheme.text = getString(R.string.light)
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                binding.tvTheme.text = getString(R.string.dark)
            }
            else -> {
                binding.tvTheme.text = getString(R.string.system)
            }
        }
    }

    fun init_onclicks() = binding.apply {
        //theme click
        llTheme.setOnClickListener {
            llTheme.setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.teal_700tr))
                dialog_theme.show()
        }
        dialog_theme.setOnCancelListener { llTheme.setBackgroundColor(0) }

        //notify
        val work_stat = WorkManager.getInstance(this@SettingsActivity).getWorkInfosForUniqueWork("allNotify").get()
        if (work_stat.size > 0 && work_stat[0].state == WorkInfo.State.ENQUEUED) swNotify.isChecked = true
        swNotify.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) WorkManager.getInstance(this@SettingsActivity).enqueueUniquePeriodicWork(
                "allNotify",
                ExistingPeriodicWorkPolicy.KEEP,
                myWorkRequest)
            if (!isChecked) WorkManager.getInstance(this@SettingsActivity).cancelUniqueWork("allNotify")
        })

        //SaveLoad click
        llSettings.setOnClickListener {
            llSettings.setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.teal_700tr))
            dialog_saveLoad.show()
        }
        dialog_saveLoad.setOnCancelListener { llSettings.setBackgroundColor(0) }
    }

    // Custom method to determine whether a service is running
//    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
//        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//
//        // Loop through the running services
//        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.name == service.service.className) {
//                // If the service is running then return true
//                return true
//            }
//        }
//        return false
//    }

    fun save(saveData: Any, file:String) {
        val dir: File = filesDir
        try {
            val file = FileOutputStream("$dir/${file}")
            val outStream = ObjectOutputStream(file)
            outStream.writeObject(saveData)
            outStream.close()
            file.close()
        } catch (e: Exception) {
            val text = getString(R.string.cantsavedata)
            Toast.makeText(this, "$text: $e", Toast.LENGTH_SHORT).show()
        }
    }
    fun load(file:String) :Any{
        val dir: File = filesDir
        try {
            val file = FileInputStream("$dir/${file}")
            val inStream = ObjectInputStream(file)
            val saveData = inStream.readObject()
            inStream.close()
            file.close()
            return saveData
        } catch (e: Exception) {
        }
        return false
    }

    private fun LimitsNotifycationWork(){

        if (!this::myWorkRequest.isInitialized) {
            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            myWorkRequest = PeriodicWorkRequest.Builder(AllNotification::class.java, 15, TimeUnit.MINUTES, 10, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            }
    }


    private fun send_limits(pc: PC):Boolean {
        val client = OkHttpClient()
        try {
            //match gpus number
            var gpusNumber = 0
            limits.forEach { (key, _) ->
                if ("${pc.name}_" in key && "${pc.name}_99999_" !in key) gpusNumber += 1
            }
            val new_limits_one_rig = hashMapOf<String, MutableMap<String, List<Int>>>()
            limits.forEach { (key, value) ->
                for (gpuNum in 0 until gpusNumber) {
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
            if (inf.get("code") != 200) return false
    } catch (e: Exception) {
        return false }
        return true
    }

    override fun onBackPressed() {
        if (this::PCList.isInitialized) {
            val editIntent = Intent()
            editIntent.putExtra(const.KEY_PCList, PCList)
            setResult(RESULT_OK, editIntent)
            finish()
        }
        super.onBackPressed()
    }

}