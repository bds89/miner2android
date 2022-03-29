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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bds89.miner2android.databinding.ActivityNodeInfoBinding
import java.io.*
import kotlin.properties.Delegates


class NodeInfoActivity : AppCompatActivity() {

    private lateinit var adapter: NodeInfoAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var MenuPCadapter: MenuPCadapter
    private lateinit var PCList: ArrayList<PC>
    private lateinit var binding: ActivityNodeInfoBinding
    private lateinit var toogle_menu: ActionBarDrawerToggle
    private lateinit var limits:HashMap<String, MutableList<Int>>


    private var position by Delegates.notNull<Int>()
    private val dataModel: DataModel by viewModels()
    var all_notification = hashMapOf<String, MutableMap<String, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNodeInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Get from Intent
        val pcl = intent.getSerializableExtra(const.KEY_PCList)
        if (pcl != null) {
            PCList = pcl as ArrayList<PC>
            position = intent.getIntExtra(const.KEY_PosNum, 0)
            if (intent.getBooleanExtra("from_notification", false)) {
                //clear notification
                //try to load all_notification
                val dir: File = this.filesDir
                try {
                    val file = FileInputStream("$dir/${const.KEY_SaveAllNotification}")
                    val inStream = ObjectInputStream(file)
                    all_notification = inStream.readObject() as HashMap<String, MutableMap<String, Int>>
                    inStream.close()
                    file.close()
                } catch (e: Exception) { }
                all_notification.remove(PCList[position].name)
            //save all_notification
                try {
                    val file = FileOutputStream("$dir/${const.KEY_SaveAllNotification}")
                    val outStream = ObjectOutputStream(file)
                    outStream.writeObject(all_notification)
                    outStream.close()
                } catch (e: Exception) { }
            }
        } else PCList = load(const.KEY_SavePC) as ArrayList<PC>
        if (PCList.size <= position) position = 0
        supportActionBar?.title = PCList[position].name
        //load or create limits
        val limitsLoad = load(const.KEY_SaveLimits)
        if (limitsLoad != false) {
            limits = limitsLoad as HashMap<String, MutableList<Int>>
        }
        else limits = hashMapOf()

        //ViewPager2
        adapter = NodeInfoAdapter(this, PCList, limits)
        viewPager = binding.pager
        viewPager.adapter = adapter
        viewPager.setCurrentItem(position, false)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(pos: Int) {
                supportActionBar?.title = PCList[pos].name
                invalidateOptionsMenu()
                menu()
                super.onPageSelected(pos)
            }
        })
        viewPager.setPageTransformer(ZoomOutPageTransformer())

        menu()
        //observers for save limits, and PCLists
        dataModel.PCList.observe(this) {
            PCList = it
            save(PCList, const.KEY_SavePC)
        }
        dataModel.limits.observe(this) {
            limits = it
            save(limits, const.KEY_SaveLimits)
        }
    }

    override fun onResume() {
        //settings background
        binding.llSettings.setBackgroundColor(0)
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.abar_node_info, menu)
        // if need handler on abar items
//        Handler().post(Runnable {
//            val view: View = findViewById(android.R.id.home)
//            if (view != null) {
//                view.setOnLongClickListener(object : View.OnLongClickListener {
//                    override fun onLongClick(v: View?): Boolean {
//                        if (binding.drawer.isDrawerOpen(GravityCompat.START)) binding.drawer.closeDrawer(
//                            GravityCompat.START)
//                        else binding.drawer.openDrawer(GravityCompat.START)
//                        return true
//                    }
//                })
//            }
//        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toogle_menu.onOptionsItemSelected(item)) return true
        return false
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (PCList[viewPager.currentItem].visibility) menu?.findItem(R.id.abarHide)?.setIcon(R.drawable.ic_baseline_visibility_24)
        else menu?.findItem(R.id.abarHide)?.setIcon(R.drawable.ic_baseline_visibility_off_24)
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

    override fun onBackPressed() {
        val editIntent = Intent()
        editIntent.putExtra(const.KEY_PCList, PCList)
        setResult(RESULT_OK, editIntent)
        finish()
        super.onBackPressed()
    }
}