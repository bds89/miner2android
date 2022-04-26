package com.bds89.miner2android

import ZoomOutPageTransformer
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bds89.miner2android.databinding.ActivityNodeInfoBinding
import com.bds89.miner2android.forRoom.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.*
import kotlin.properties.Delegates


class NodeInfoActivity : AppCompatActivity() {

    private lateinit var adapter: NodeInfoAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var MenuPCadapter: MenuPCadapter
    private lateinit var PCList: ArrayList<PC>
    private lateinit var binding: ActivityNodeInfoBinding
    private lateinit var toogle_menu: ActionBarDrawerToggle



    private var position by Delegates.notNull<Int>()
    private val dataModel: DataModel by viewModels()
    private var limits = arrayListOf<LimitEntity?>()
    var all_notification = hashMapOf<String, MutableMap<String, Int>>()
    var last_resonce_time = hashMapOf<String, Int>()

    //DB
    val db: AppDatabase = App.instance.database
    val PCsDao = db.pcsDao()
    val limitDao = db.LimitDao()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNodeInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //observers for save limits, and PCLists
        dataModel.PCList.observe(this) {
            PCList = it
            lifecycleScope.launch {
                PCList.forEach {
                    PCsDao?.update(PCsEntity.fromPC(it))
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            //Get from Intent

            val pcl = intent.getSerializableExtra(const.KEY_PCList)
            if (pcl != null) {
                PCList = pcl as ArrayList<PC>
                position = intent.getIntExtra(const.KEY_PosNum, 0)
                if (intent.getBooleanExtra("from_notification", false)) {
                    val title = intent.getStringExtra("title")
                    //clear notification
                    //try to load all_notification
                    val dir: File = this@NodeInfoActivity.filesDir
                    try {
                        val file = FileInputStream("$dir/${const.KEY_SaveAllNotification}")
                        val inStream = ObjectInputStream(file)
                        all_notification =
                            inStream.readObject() as HashMap<String, MutableMap<String, Int>>
                        inStream.close()
                        file.close()
                    } catch (e: Exception) {
                    }
                    all_notification.remove(PCList[position].name)
                    //try to load last_resonce_time
                    try {
                        val file = FileInputStream("$dir/${const.KEY_SaveLastResponce}")
                        val inStream = ObjectInputStream(file)
                        last_resonce_time = inStream.readObject() as HashMap<String, Int>
                        inStream.close()
                        file.close()
                    } catch (e: Exception) {
                    }
                    last_resonce_time.remove(title)
                    //save all_notification
                    try {
                        val file = FileOutputStream("$dir/${const.KEY_SaveAllNotification}")
                        val outStream = ObjectOutputStream(file)
                        outStream.writeObject(all_notification)
                        outStream.close()
                    } catch (e: Exception) {
                    }
                    //save last_resonce_time
                    try {
                        val file = FileOutputStream("$dir/${const.KEY_SaveLastResponce}")
                        val outStream = ObjectOutputStream(file)
                        outStream.writeObject(last_resonce_time)
                        outStream.close()
                        file.close()

                    } catch (e: Exception) {
                    }
                }
            } else PCList = async { PCsEntity.listToPCList(PCsDao?.getAll()) }.await()

            if (PCList.size <= position) position = 0
            supportActionBar?.title = PCList[position].name
            val limits_ = ArrayList(limitDao?.getAll())
            if (limits_ != null) limits = limits_

            //ViewPager2
            adapter = NodeInfoAdapter(this@NodeInfoActivity, PCList, limits)
            viewPager = binding.pager
            viewPager.adapter = adapter
            viewPager.setCurrentItem(position, false)
            viewPager.setPageTransformer(ZoomOutPageTransformer())
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(pos: Int) {
                    supportActionBar?.title = PCList[pos].name
                    invalidateOptionsMenu()
                    menu()
                    super.onPageSelected(pos)
                }
            })
            menu()
        }
    }

    override fun onResume() {
        //settings background
        binding.llSettings.setBackgroundColor(0)
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.abar_node_info, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toogle_menu.onOptionsItemSelected(item)) return true
        return false
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (this::viewPager.isInitialized) {
            if (PCList[viewPager.currentItem].visibility) menu?.findItem(R.id.abarHide)?.setIcon(R.drawable.ic_baseline_visibility_24)
            else menu?.findItem(R.id.abarHide)?.setIcon(R.drawable.ic_baseline_visibility_off_24)
        } else {
            if (PCList[position].visibility) menu?.findItem(R.id.abarHide)?.setIcon(R.drawable.ic_baseline_visibility_24)
            else menu?.findItem(R.id.abarHide)?.setIcon(R.drawable.ic_baseline_visibility_off_24)
        }
        return true
    }
    private fun menu() {
        binding.apply {
            //Menu adapter
            menuPCRecycler.layoutManager = LinearLayoutManager(this@NodeInfoActivity)

            MenuPCadapter = MenuPCadapter(PCList, viewPager.currentItem, object :MenuPCadapter.ItemClickListener{
                override fun itemClicked(position: Int) {
                    performItemClick(position)
                }
            })
            menuPCRecycler.adapter = MenuPCadapter
        }
        toogle_menu = ActionBarDrawerToggle(this, binding.drawer, R.string.open, R.string.close)
        binding.drawer.addDrawerListener(toogle_menu)
        toogle_menu.syncState()
        supportActionBar?.title = PCList[viewPager.currentItem].name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun performItemClick(position: Int) {
        viewPager.setCurrentItem(position, true)
    }

    //go to settings
    fun go_to_settings(view: View?){
        binding.llSettings.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
        val i = Intent(this, SettingsActivity::class.java).apply {}
        startActivity(i)
    }

    override fun onBackPressed() {
        val editIntent = Intent()
        editIntent.putExtra(const.KEY_PCList, PCList)
        setResult(RESULT_OK, editIntent)
        finish()
        super.onBackPressed()
    }
}