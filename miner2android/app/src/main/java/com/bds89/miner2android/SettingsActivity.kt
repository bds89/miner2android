package com.bds89.miner2android

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat.recreate
import androidx.core.content.ContextCompat
import com.bds89.miner2android.databinding.ActivitySettingsBinding
import com.bds89.miner2android.databinding.DialogLangBinding
import com.bds89.miner2android.databinding.DialogThemeBinding
import java.io.*
import java.util.*


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding:ActivitySettingsBinding
    private lateinit var dialog_theme: Dialog
    private lateinit var settings: HashMap<String, String>


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
//        dialog_lang_inflate()

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

    
//    private fun dialog_lang_inflate() {
//        dialog_lang = Dialog(this)
//
//        // Передайте ссылку на разметку
//        val binding_d = DialogLangBinding.inflate(layoutInflater)
//        dialog_lang.setContentView(binding_d.root)
//
//        if (saveData.settings["lang"] != null) {
//            when (saveData.settings["lang"]) {
//                "en" -> {
//                    binding.tvLang.text = "English"
//                    binding_d.rg.check(binding_d.rbEng.id)
//                }
//                "ru" -> {
//                    binding.tvLang.text = "Русский"
//                    binding_d.rg.check(binding_d.rbRu.id)
//                }
//                else -> {
//                    binding.tvLang.text = "System"
//                    binding_d.rg.check(binding_d.rbSystem.id)
//                }
//            }
//        }
//            binding_d.rg.setOnCheckedChangeListener { radioGroup, i ->
//                when (i) {
//                    binding_d.rbSystem.id -> saveData.settings["lang"] = "root"
//                    binding_d.rbEng.id -> saveData.settings["lang"] = "en"
//                    binding_d.rbRu.id -> saveData.settings["lang"] = "ru"
//                }
//                save(saveData)
//                setAppLocale(saveData.settings["lang"]!!)
//                recreate()
//            }
//    }

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
            llTheme.setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.teal_700))
                dialog_theme.show()
        }
        dialog_theme.setOnCancelListener { llTheme.setBackgroundColor(0) }
        //language click
//        llLang.setOnClickListener {
//            llLang.setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.teal_700))
//            dialog_lang.show()
//        }
//        dialog_lang.setOnCancelListener { llLang.setBackgroundColor(0) }
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

    //change locale at chose
    fun Context.setAppLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
//    override fun attachBaseContext(newBase: Context) {
//        if (this::saveData.isInitialized && saveData != null && saveData!!.settings["lang"] != null) {
//            super.attachBaseContext(ContextWrapper(newBase.setAppLocale(saveData!!.settings["lang"]!!)))
//        } else super.attachBaseContext(ContextWrapper(newBase.setAppLocale("root")))
//    }
}