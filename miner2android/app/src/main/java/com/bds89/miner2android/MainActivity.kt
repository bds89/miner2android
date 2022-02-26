package com.bds89.miner2android

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bds89.miner2android.databinding.ActivityMainBinding
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var PCadapter: PCadapter
    private lateinit var MenuPCadapter: MenuPCadapter
    private lateinit var settings: HashMap<String, String>
    private var spanCount = 2

    lateinit var itemEditLauncher: ActivityResultLauncher<Intent>
    lateinit var itemInfoLauncher: ActivityResultLauncher<Intent>
    lateinit var PCList: ArrayList<PC>
    lateinit var toogle_menu: ActionBarDrawerToggle


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //load settings
        val settingLoad = load(const.KEY_SaveSettings)
        if (settingLoad != false) {
            settings = settingLoad as HashMap<String, String>
            //apply settings
            //theme
            if (settings.containsKey("theme")) settings["theme"]?.let {
                AppCompatDelegate.setDefaultNightMode(
                    it.toInt())
            }
        } else settings = hashMapOf<String, String>()
        //load PCList
        val PCListLoad = load(const.KEY_SavePC)
        if (PCListLoad != false) {
            PCList = PCListLoad as ArrayList<PC>
        }
        else {
            binding.tvHello.visibility = View.VISIBLE
            PCList = ArrayList<PC>()
        }

        init()
        onResult()
        init_menu()
    }

    override fun onStart() {
        //Menu adapter
        binding.menuPCRecycler.layoutManager = LinearLayoutManager(this@MainActivity)
        MenuPCadapter = MenuPCadapter(PCadapter.PCList, 99999, object :MenuPCadapter.ItemClickListener{
            override fun itemClicked(position: Int) {
                performLeftMenuClick(position)
            }
        })
        binding.menuPCRecycler.adapter = MenuPCadapter
        PCadapter.refreshPCs()
        //settings background
        binding.llSettings.setBackgroundColor(0)
        super.onStart()
    }
    private fun init_menu(){
        toogle_menu = ActionBarDrawerToggle(this, binding.drawer, R.string.open, R.string.close)
        binding.drawer.addDrawerListener(toogle_menu)
        toogle_menu.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.abar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toogle_menu.onOptionsItemSelected(item)) return true
        when (item.itemId) {
            R.id.abarRefresh -> PCadapter.refreshPCs()
        }
        return true
    }
    private fun init(){
        //chose spancount for main recycler
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) spanCount *= 2
        if (PCList.size > 0 && PCList.size < spanCount) spanCount = PCList.size
        binding.apply {
            //Main adapter
            PCRecycler.layoutManager = GridLayoutManager(this@MainActivity, spanCount)
            PCadapter = PCadapter(PCList = PCList, optionsMenuClickListener=object : PCadapter.OptionsMenuClickListener{
                override fun onOptionsMenuClicked(position: Int):Boolean{
                    return performOptionsMenuClick(position)
                }
            }, itemClickListener=object : PCadapter.ItemClickListener{
                override fun itemClicked(position: Int){
                    performItemClick(position)
                }
            })
            PCRecycler.adapter = PCadapter

            //button add pc
            btAdd.setOnClickListener {
                if (binding.tvHello.visibility == View.VISIBLE) binding.tvHello.visibility = View.GONE
                val i = Intent(this@MainActivity, itemEditActivity::class.java).apply {
                    putExtra(const.KEY_PCList, PCList)
                }
                itemEditLauncher.launch(i)
            }
            //swipe refresh
            swipeContainer.setOnRefreshListener {
                PCadapter.refreshPCs()
                swipeContainer.isRefreshing = false
            }
        }
    }
    //Item click
    private fun performItemClick(position: Int) {
        val i = Intent(this, NodeInfoActivity::class.java).apply {
            putExtra(const.KEY_PCList, PCList)
            putExtra(const.KEY_PosNum, position)
        }
        itemInfoLauncher.launch(i)
    }
    //Item menu
    private fun performOptionsMenuClick(position: Int): Boolean {
        val popupMenu = PopupMenu(this , binding.PCRecycler[position].findViewById(R.id.tv_menu))
        popupMenu.inflate(R.menu.item_menu)
        popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener{
            override fun onMenuItemClick(item: MenuItem?): Boolean {
                when(item?.itemId){
                    R.id.itemMenuEdit -> {
                        // here are the logic to edit an item from the list
                        val i = Intent(this@MainActivity, itemEditActivity::class.java).apply {
                            putExtra(const.KEY_PC_item, PCadapter.PCList[position])
                            putExtra(const.KEY_PCList, PCList)
                        }
                        itemEditLauncher.launch(i)
                        return true
                    }
                    R.id.itemMenuDelete -> {
                        var text = "${getString(R.string.deleted)}: ${PCadapter.PCList[position].name}"
                        if (!PCadapter.delPC(PCadapter.PCList[position].id)) text = getString(R.string.delete_error)
                        Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
                        MenuPCadapter.refreshList(PCadapter.PCList)
                        //chose spancount for main recycler
                        if (PCList.size > 0 && PCList.size < spanCount) spanCount = PCList.size
                        binding.PCRecycler.layoutManager = GridLayoutManager(this@MainActivity, spanCount)
                        //save PCList
                        save(PCList, const.KEY_SavePC)
                        return true
                    }
                }
                return false
            }
        })
        popupMenu.show()
        return true

    }
    //Left menu click
    private fun performLeftMenuClick(position:Int){
        val i = Intent(this, NodeInfoActivity::class.java).apply {
            putExtra(const.KEY_PCList, PCList)
            putExtra(const.KEY_PosNum, position)
        }
        itemInfoLauncher.launch(i)
    }
    //go to settings
    fun go_to_settings(view: View?){
        binding.llSettings.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
        val i = Intent(this, SettingsActivity::class.java).apply {
            putExtra(const.KEY_SaveSettings, settings)
        }
        startActivity(i)
    }

    fun onResult(){
        itemEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                val pc: PC = it.data?.getSerializableExtra(const.KEY_PC_item) as PC
                if (it.data!!.getBooleanExtra(const.KEY_edit_item, false)) {
                    PCadapter.editPC(pc)
                    MenuPCadapter.refreshList(PCadapter.PCList)
                    //save PCList
                    save(PCList, const.KEY_SavePC)

                    PCadapter.refreshPCs()
                }
                else {
                    PCadapter.addPC(pc)
                    MenuPCadapter.refreshList(PCadapter.PCList)

                    //chose spancount for main recycler
                    spanCount = 2
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) spanCount *= 2
                    if (PCList.size > 0 && PCList.size < spanCount) spanCount = PCList.size
                    binding.PCRecycler.layoutManager = GridLayoutManager(this@MainActivity, spanCount)
                    //save PCList
                    save(PCList, const.KEY_SavePC)

                    PCadapter.refreshPCs()
                }

            }
        }
        itemInfoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                PCList = it.data?.getSerializableExtra(const.KEY_PCList) as ArrayList<PC>
            }
        }
    }

    override fun onBackPressed() {
        save(PCList, const.KEY_PCList)
        save(settings, const.KEY_SaveSettings)
        finish()
        super.onBackPressed()
    }
//    //change locale at start
//    fun Context.setAppLocale(language: String) {
//        val locale = Locale(language)
//        Locale.setDefault(locale)
//        val config = resources.configuration
//        config.setLocale(locale)
//        config.setLayoutDirection(locale)
//        createConfigurationContext(config)
//        resources.updateConfiguration(config, resources.displayMetrics)
//    }
}