package com.bds89.miner2android

import DepthPageTransformer
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.bds89.miner2android.databinding.ActivityItemEditBinding
import com.google.android.material.textfield.TextInputLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.*
import kotlin.collections.ArrayList


class itemEditActivity : AppCompatActivity() {
    lateinit var binding: ActivityItemEditBinding
    private var itemID: Int? = null
    var save_button: Boolean = true
    private lateinit var MenuPCadapter: MenuPCadapter
    lateinit var toogle_menu: ActionBarDrawerToggle
    lateinit var PCList:ArrayList<PC>
    lateinit var pc: PC
    var oldName = ""

    //ViewPager2
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: IconNodeAdapter
    var inIP = ""

    var image_num_in_fragment: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent?.getSerializableExtra(const.KEY_PC_item) != null) {
            pc = intent.getSerializableExtra(const.KEY_PC_item) as PC
            supportActionBar?.title = pc.name
        } else {
            supportActionBar?.title = getString(R.string.add_pc)
        }

        PCList = intent.getSerializableExtra(const.KEY_PCList) as ArrayList<PC>

        //ViewPager2
        adapter = IconNodeAdapter(this)
        viewPager = binding.pager
        viewPager.adapter = adapter
        if (this::pc.isInitialized) {
            viewPager.currentItem = pc.imageID
            image_num_in_fragment = pc.imageID
        }
        viewPager.setPageTransformer(DepthPageTransformer())

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                image_num_in_fragment = viewPager.currentItem
            }
        })

        initActivity()
        initButtons()
        init_menu()
        change_button_color(binding.tilPort)
        check_data_for_button_collor()
    }

    override fun onResume() {
        //settings background
        binding.llSettings.setBackgroundColor(0)
        super.onResume()
    }

    private fun init_menu(){
        toogle_menu = ActionBarDrawerToggle(this, binding.drawer, R.string.open, R.string.close)
        binding.drawer.addDrawerListener(toogle_menu)
        toogle_menu.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.abar_item_edit, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (save_button) menu?.findItem(R.id.abar_save)?.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this@itemEditActivity, R.color.white)))
        else menu?.findItem(R.id.abar_save)?.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this@itemEditActivity, R.color.gray)))
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toogle_menu.onOptionsItemSelected(item)) return true
        when (item.itemId) {
            R.id.abar_save -> with(binding){
                val check_name = check_name()
                if ((etExIP.text.isNullOrEmpty() && etInIP.text.isNullOrEmpty()) || etPort.text.isNullOrEmpty() || check_name) {
                    if (etExIP.text.isNullOrEmpty()) tilExIP.setError(getString(R.string.need_enter_an_ip))
                    if (etInIP.text.isNullOrEmpty()) tilInIP.setError(getString(R.string.need_enter_an_iip))
                    if (etPort.text.isNullOrEmpty()) tilPort.setError(getString(R.string.need_enter_port))
                    if (check_name) tilName.setError(getString(R.string.name_unique))
                } else {
                    val editIntent = Intent()
                    var pc = PC(
                        name = etName.text.toString(),
                        imageID = image_num_in_fragment,
                        ex_IP = etExIP.text.toString(),
                        port = etPort.text.toString(),
                        in_IP = etInIP.text.toString(),
                        in_port = etInport.text.toString())
                    pc.upass = etPass.text.toString()
                    if (itemID != null) {
                        pc.id = itemID as Int
                        editIntent.putExtra(const.KEY_edit_item, true)
                        GlobalScope.launch(Dispatchers.IO) {
                            ChangeNameEverywhere(oldName, pc)
                        }
                    }
                    editIntent.putExtra(const.KEY_PC_item, pc)
                    setResult(RESULT_OK, editIntent)
                    finish()
                }
            }
        }
        return true
    }

    private fun initActivity() {
        if (this::pc.isInitialized) {
            binding.apply {
                itemID = pc.id
                etName.setText(pc.name)
                //for changeNameEverywhere
                oldName = pc.name

                if (pc.ex_IP != "") etExIP.setText(pc.ex_IP)
                if (pc.port != "") etPort.setText(pc.port)
                if (pc.in_IP != "") {
                    etInIP.setText(pc.in_IP)
                    cbUseasgateway.isChecked = true
                    inIP = pc.in_IP
                    check_box()
                }
                if (!pc.in_port.isEmpty()) etInport.setText((pc.in_port))
            }
        }

        //Menu adapter
        binding.apply {
            menuPCRecycler.layoutManager = LinearLayoutManager(this@itemEditActivity)
            MenuPCadapter =
                MenuPCadapter(PCList, 99999, object : MenuPCadapter.ItemClickListener {
                    override fun itemClicked(position: Int) {
                        performLeftMenuClick(position)
                    }
                })
            menuPCRecycler.adapter = MenuPCadapter
        }
    }
    //Left menu click
    private fun performLeftMenuClick(position:Int){
        val i = Intent(this, NodeInfoActivity::class.java).apply {
            putExtra(const.KEY_PCList, PCList)
            putExtra(const.KEY_PosNum, position)
        }
        this.startActivity(i)
    }

    private fun initButtons() = with(binding){
        cbUseasgateway.setOnClickListener(){check_box()}
    }
    fun check_box() = with(binding){
        if (cbUseasgateway.isChecked) {
            tilExIP.hint = getString(R.string.gateway_PC_IP)
            tilPort.hint = getString(R.string.gateway_PC_Port)
            tilPass.hint = getString(R.string.gateway_Pass)
            tilInIP.visibility = View.VISIBLE
            tilInport.visibility = View.VISIBLE
            if (!inIP.isNullOrEmpty()) etInIP.setText(inIP)
        } else {
            tilExIP.hint = "IP"
            tilPort.hint = getString(R.string.port)
            tilPass.hint = getString(R.string.password)
            tilInIP.visibility = View.GONE
            tilInport.visibility = View.GONE
            inIP = etInIP.text.toString()
            etInIP.setText(null)
        }
    }
    fun change_button_color(view: TextInputLayout){
        view.setError(null)
                with(binding){
                    save_button = (!etExIP.text.isNullOrEmpty() || !etInIP.text.isNullOrEmpty()) && !etPort.text.isNullOrEmpty()
        }
        invalidateOptionsMenu()
    }

    private fun check_data_for_button_collor(){
        binding.apply {
            etExIP.doOnTextChanged { text, start, before, count -> run { change_button_color(tilExIP) } }
            etInIP.doOnTextChanged { text, start, before, count -> run { change_button_color(tilInIP) } }
            etPort.doOnTextChanged { text, start, before, count -> run { change_button_color(tilPort) } }
            etName.doOnTextChanged { text, start, before, count -> run { change_button_color(tilName) } }

        }
    }

    private fun check_name(): Boolean{
        if (this::pc.isInitialized) return false
        for (PC in PCList) {
            if (PC.name == binding.etName.text.toString()) return true
        }
        return false
    }
    //go to settings
    fun go_to_settings(view: View?){
        binding.llSettings.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
        val i = Intent(this, SettingsActivity::class.java).apply {
        }
        startActivity(i)
    }

    suspend fun ChangeNameEverywhere(old:String, pc: PC) {

        val dir: File = filesDir
        var limits = hashMapOf<String, MutableList<Int>>()
        val newLimits = hashMapOf<String, MutableList<Int>>()
        val client = OkHttpClient()

        try {
            //load limits
            val file = FileInputStream("$dir/${const.KEY_SaveLimits}")
            val inStream = ObjectInputStream(file)
            limits = inStream.readObject() as HashMap<String, MutableList<Int>>
            inStream.close()
            file.close()
            //change limits
            limits.forEach { key, value->
                if ("${old}_" in key) {
                    val new_key = "${pc.name}_"+key.drop("${old}_".length)
                    newLimits.put(new_key, value)
                } else newLimits.put(key, value)
            }
            //save newLimits
            val fileSave = FileOutputStream("$dir/${const.KEY_SaveLimits}")
            val outStream = ObjectOutputStream(fileSave)
            outStream.writeObject(newLimits)
            outStream.close()
            file.close()
            //send newLimits
            val new_limits_one_rig = hashMapOf<String, MutableMap<String, List<Int>>>()
            //match gpus number
            var gpusNumber = 0
            newLimits.forEach { (key, _) ->
                if ("${pc.name}_" in key && "${pc.name}_99999_" !in key) gpusNumber += 1
            }
            newLimits.forEach { (key, value) ->
                //system parameters
                if ("${pc.name}_99999_" in key) {
                    val new_key = key.drop("${pc.name}_${99999}_".length)
                    if (new_limits_one_rig.containsKey("99999")) new_limits_one_rig["99999"]?.put(new_key, value)
                    else new_limits_one_rig.put("99999", mutableMapOf(new_key to value))
                    return@forEach
                }
                for (gpuNum in 0 until gpusNumber) {
                    if ("${pc.name}_${gpuNum}_" in key) {
                        val new_key = key.drop("${pc.name}_${gpuNum}_".length)
                        if (new_limits_one_rig.containsKey("$gpuNum")) new_limits_one_rig["$gpuNum"]?.put(new_key, value)
                        else new_limits_one_rig.put("$gpuNum", mutableMapOf(new_key to value))
                    } else break
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
                val resp = client.newCall(request).execute()
                inf = JSONObject(resp.body!!.string())
            if (inf.get("code") != 200) Toast.makeText(this, inf.toString(), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { }
        //try to load and save last_resonce_time
        try {
            val file = FileInputStream("$dir/${const.KEY_SaveLastResponce}")
            val inStream = ObjectInputStream(file)
            val last_resonce_time = inStream.readObject() as HashMap<String, Int>
            inStream.close()
            file.close()
            last_resonce_time.remove(oldName)

            val fileSave = FileOutputStream("$dir/${const.KEY_SaveLastResponce}")
            val outStream = ObjectOutputStream(fileSave)
            outStream.writeObject(last_resonce_time)
            outStream.close()
            fileSave.close()
        } catch (e: Exception) {}
    }
}