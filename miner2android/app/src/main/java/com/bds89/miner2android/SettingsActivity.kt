package com.bds89.miner2android

import android.app.Dialog
import android.content.Intent
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
import com.bds89.miner2android.forRoom.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
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


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding:ActivitySettingsBinding
    private lateinit var dialog_theme: Dialog
    private lateinit var dialog_saveLoad: Dialog
    private lateinit var settings: HashMap<String, String>
    private lateinit var myWorkRequest: PeriodicWorkRequest
    private lateinit var getFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var PCList :ArrayList<PC>
    private lateinit var limits:ArrayList<LimitEntity?>

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
        //DB
        val db: AppDatabase = App.instance.database
        val PCsDao = db.pcsDao()
        val limitDao = db.LimitDao()

        val dir: File = filesDir
        // Передайте ссылку на разметку
        val binding_d = DialogSaveloadBinding.inflate(layoutInflater)
        with(binding_d) {
            tvSave.setOnClickListener {
                GlobalScope.launch(Dispatchers.Main) {
                    var all_settings = hashMapOf<String, Any>()
                    val sett = load(const.KEY_SaveSettings)
                    if (sett != null) {
                        all_settings.put(
                            "settings",
                            sett as HashMap<String, String>
                        )
                    }
                    //PClist
                    val PCList = async { withContext(Dispatchers.IO) { PCsEntity.listToPCList(PCsDao?.getAll()) } }.await()
                    all_settings.put("PCList", PCList)
                    //Limits
                    val limits = async { withContext(Dispatchers.IO) { ArrayList(limitDao?.getAll()) } }.await()
                    all_settings.put("limits", limits)
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
                        file
                    )
                    //create intent
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "*/*"
                    }
                    startActivity(Intent.createChooser(shareIntent, null))
                    dialog_saveLoad.cancel()
                }
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
                                PCList = all_settings["PCList"]!! as ArrayList<PC>
                                GlobalScope.launch(Dispatchers.IO) {
                                    PCsDao?.deleteAll()
                                    PCList.forEach {
                                        PCsDao?.insert(PCsEntity.fromPC(it))
                                    }
                                }
                            }
                            binding.pbApplySettings.progress = 10
                            //limits
                            if (all_settings.containsKey("limits") && all_settings["limits"] != false && all_settings["limits"] != null) {
                                limits = all_settings["limits"]!! as ArrayList<LimitEntity?>

                                GlobalScope.launch(Dispatchers.IO) {
                                    limitDao?.deleteAll()
                                    limits.forEach {
                                        limitDao?.insert(it)
                                    }
                                }

                                val one_pc_progress = (90/PCList.size).toInt()
                                var send_succes = true
                                PCList.forEach { pc->
                                    val job = GlobalScope.launch(Dispatchers.IO) {
                                        send_succes = send_limits(pc)
                                    }
                                    GlobalScope.launch(Dispatchers.Main) {
                                        job.join()
                                        if (!send_succes) {
                                            Toast.makeText(
                                                this@SettingsActivity,
                                                "Can't load ${pc.name}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        binding.pbApplySettings.progress += one_pc_progress
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
    fun load(file:String) :Any?{
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
        return null
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
            val limitsOneRig = limits.filter { it?.pcName == pc.name }
            val new_limits_one_rig = hashMapOf<String, MutableMap<String, List<Any>>>()
            limitsOneRig.forEach {
                if (it != null) {
                    if (new_limits_one_rig.containsKey("${it.ptype}")) new_limits_one_rig["${it.ptype}"]?.put(
                        it.pname,
                        listOf(it.value, it.above, it.datetime)
                    )
                    else new_limits_one_rig.put(
                        "${it.ptype}",
                        mutableMapOf(it.pname to listOf(it.value, it.above, it.datetime))
                    )
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