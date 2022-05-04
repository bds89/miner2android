package com.bds89.miner2android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bds89.miner2android.databinding.ActivityMainBinding
import com.bds89.miner2android.forRoom.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.*
import java.lang.Math.round
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var PCadapter: PCadapter
    private lateinit var MenuPCadapter: MenuPCadapter
    private lateinit var settings: HashMap<String, String>
    //for bottomSheet
    private val client = OkHttpClient()
    private var responce:String? = null
    var CURList = ArrayList<CUR>()
    var dateCURupdate = Date(0)
    var userCURList = ArrayList<String>()
    var userCURListforAdapter = ArrayList<CUR>()
    var swipeEnabled = false
    private lateinit var BottomSheetAdapter:BottomSheetAdapter
    private lateinit var NamesAndTickers : ArrayList<String>
    private lateinit var info: JSONObject
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>



    private var spanCount = 2

    private val dataModel: DataModel by viewModels()
    var PCList = ArrayList<PC>()

    lateinit var itemEditLauncher: ActivityResultLauncher<Intent>
    lateinit var itemInfoLauncher: ActivityResultLauncher<Intent>
    lateinit var SettingsLauncher: ActivityResultLauncher<Intent>
    lateinit var toogle_menu: ActionBarDrawerToggle

    //DB
    private val db: AppDatabase = App.instance.database
    private val PCsDao = db.pcsDao()
    private val curDao = db.CURDao()

    override fun onCreate(savedInstanceState: Bundle?) {
        //load settings
        val settingLoad = load(const.KEY_SaveSettings)
        if (settingLoad != false) {
            settings = settingLoad as HashMap<String, String>
            //apply settings
            //theme
            if (settings.containsKey("theme")) settings["theme"]?.let {
                AppCompatDelegate.setDefaultNightMode(
                    it.toInt()
                )
            }

        } else settings = hashMapOf<String, String>()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //load PCList

        lifecycleScope.launch { dataModel.PCList.value = PCsEntity.listToPCList(PCsDao?.getAll())}
        //observers for save limits, and PCLists

        init()
        onResult()
        init_menu()
        createNotificationChannel()
        bottomSheet()
        //observe PCList
        dataModel.PCList.observe(this) {
            PCList = it
            if (PCadapter.PCList != PCList) PCadapter.updatePCList(PCList)
            //chose spancount for main recycler
            spanCount = 2
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) spanCount *= 2
            if (PCList.size > 0 && PCList.size < spanCount) spanCount = PCList.size
            binding.PCRecycler.layoutManager = GridLayoutManager(this@MainActivity, spanCount)
            //refresh leftside menu
            MenuPCadapter.refreshList(PCadapter.PCList)
            if (PCList.isNullOrEmpty()) binding.tvHello.visibility = View.VISIBLE
            else binding.tvHello.visibility = View.GONE
        }
    }

    override fun onStart() {
        binding.btAdd.animation =
            AnimationUtils.loadAnimation(this@MainActivity, R.anim.rotate1)
        //Menu adapter
        binding.menuPCRecycler.layoutManager = LinearLayoutManager(this@MainActivity)
        MenuPCadapter =
            MenuPCadapter(PCadapter.PCList, 99999, object : MenuPCadapter.ItemClickListener {
                override fun itemClicked(position: Int) {
                    performLeftMenuClick(position)
                }
            })
        binding.menuPCRecycler.adapter = MenuPCadapter
        //settings background
        binding.llSettings.setBackgroundColor(0)
        super.onStart()
    }

    private fun init_menu() {
        toogle_menu = ActionBarDrawerToggle(this, binding.drawer, R.string.open, R.string.close)
        binding.drawer.addDrawerListener(toogle_menu)
        toogle_menu.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun save(saveData: Any, file: String) {
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

    fun load(file: String): Any {
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

    private fun init() {
        //chose spancount for main recycler
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) spanCount *= 2
        if (PCList.size > 0 && PCList.size < spanCount) spanCount = PCList.size
        binding.apply {
            //Main adapter
            PCRecycler.layoutManager = GridLayoutManager(this@MainActivity, spanCount)
            PCadapter = PCadapter(
                PCList = PCList,
                optionsMenuClickListener = object : PCadapter.OptionsMenuClickListener {
                    override fun onOptionsMenuClicked(position: Int, ivPC: ImageView): Boolean {
                        return performOptionsMenuClick(position, ivPC)
                    }
                },
                itemClickListener = object : PCadapter.ItemClickListener {
                    override fun itemClicked(position: Int) {
                        performItemClick(position)
                    }
                },
                dataModel = dataModel)
            PCRecycler.adapter = PCadapter
//            PCRecycler.animation

            //button add pc
            btAdd.setOnClickListener {
                if (binding.tvHello.visibility == View.VISIBLE) binding.tvHello.visibility =
                    View.GONE
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
    private fun performOptionsMenuClick(position: Int, ivPC: ImageView): Boolean {
        val animationRotateCenter = AnimationUtils.loadAnimation(this, R.anim.rotate1)
        ivPC.startAnimation(animationRotateCenter)
        val popupMenu = PopupMenu(this, binding.PCRecycler[position].findViewById(R.id.tv_menu))
        popupMenu.inflate(R.menu.item_menu)
        popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.itemMenuOpen -> {
                        val i = Intent(this@MainActivity, NodeInfoActivity::class.java).apply {
                            putExtra(const.KEY_PCList, PCList)
                            putExtra(const.KEY_PosNum, position)
                        }
                        itemInfoLauncher.launch(i)
                        return true
                    }
                    R.id.itemMenuEdit -> {
                        val i = Intent(this@MainActivity, itemEditActivity::class.java).apply {
                            putExtra(const.KEY_PC_item, PCadapter.PCList[position])
                            putExtra(const.KEY_PCList, PCList)
                        }
                        itemEditLauncher.launch(i)
                        return true
                    }
                    R.id.itemMenuDelete -> {
                        var text =
                            "${getString(R.string.deleted)}: ${PCadapter.PCList[position].name}"
                        if (!PCadapter.delPC(PCadapter.PCList[position].id)) text =
                            getString(R.string.delete_error)
                        Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
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
    private fun performLeftMenuClick(position: Int) {
        val i = Intent(this, NodeInfoActivity::class.java).apply {
            putExtra(const.KEY_PCList, PCList)
            putExtra(const.KEY_PosNum, position)
        }
        itemInfoLauncher.launch(i)
    }

    //go to settings
    fun go_to_settings(view: View?) {
        binding.llSettings.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
        val i = Intent(this, SettingsActivity::class.java).apply {
            putExtra(const.KEY_SaveSettings, settings)
        }
        SettingsLauncher.launch(i)
    }

    fun onResult() {
        itemEditLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK && it.data != null) {
                    val pc: PC = it.data?.getSerializableExtra(const.KEY_PC_item) as PC
                    if (it.data!!.getBooleanExtra(const.KEY_edit_item, false)) {
                        PCadapter.editPC(pc)
                    } else {
                        PCadapter.addPC(pc)
                    }
                }
            }
        itemInfoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK && it.data != null) {
                    PCList = it.data?.getSerializableExtra(const.KEY_PCList) as ArrayList<PC>
                }
            }

        SettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                if (it.data?.getSerializableExtra(const.KEY_PCList) != null) {
                    PCList = it.data?.getSerializableExtra(const.KEY_PCList) as ArrayList<PC>
                    binding.tvHello.visibility = View.GONE
                }
                settings = it.data?.getSerializableExtra(const.KEY_SaveSettings) as HashMap<String, String>
                val llBottomSheet = findViewById(R.id.bottom_sheet) as LinearLayout
                val bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet)
                if (settings.containsKey("CoinMarketCupToken")) {
                    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN &&
                        !this::NamesAndTickers.isInitialized) {
                            bottomSheet()
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
                else {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
                init()
            }
        }
    }

    override fun onBackPressed() {
//        save(PCList, const.KEY_PCList)
        save(settings, const.KEY_SaveSettings)
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            finish()
        }
    }

    //Notification Chanell
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "All Notification"
            val descriptionText = ""
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(const.CHANNEL_ID_LIMITS, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun bottomSheet() {
// получение вью нижнего экрана
        val llBottomSheet = findViewById(R.id.bottom_sheet) as LinearLayout

// настройка поведения нижнего экрана
        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet)
        val tv_error = findViewById<TextView>(R.id.tv_error)

//если нет токена
        if (settings["CoinMarketCupToken"].isNullOrEmpty()) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            return
        }
// Autocomplete text view
        NamesAndTickers = arrayListOf<String>()
        val adapterACTV: ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, NamesAndTickers)
        val autocompletetv = findViewById<View>(R.id.autocomplete) as AutoCompleteTextView
        autocompletetv.setAdapter(adapterACTV)
        autocompletetv.setOnItemClickListener { parent, view, position, id ->
            val curNameandSymbol = parent.getItemAtPosition(position)
            val curIndex = NamesAndTickers.indexOf(curNameandSymbol)
            val cur = CURList[curIndex]
            GlobalScope.launch { curDao?.insert(CUREntity(CURsymbol = cur.symbol)) }
            var needRefresh = false
            if (userCURList.isNullOrEmpty()) needRefresh = true
            userCURList.add(cur.symbol)
            if (cur != null && !userCURListforAdapter.contains(cur)) {
                BottomSheetAdapter.addCur(cur, needRefresh)
            }
            autocompletetv.setText("")
             }

        //long click btADD
        binding.btAdd.setOnLongClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return@setOnLongClickListener true
        }

        //lock SWIPE
        val ivLock = findViewById<ImageView>(R.id.iv_lock)
        ivLock.setOnClickListener {
            swipeEnabled = if (swipeEnabled) {
                ivLock.setImageResource(R.drawable.ic_baseline_lock_24)
                ivLock.animate().alpha(0.5f).setDuration(300).start()
                false
            } else {
                ivLock.setImageResource(R.drawable.ic_baseline_lock_open_24)
                ivLock.animate().alpha(1f).setDuration(300).start()
                true
            }
        }

        //CURadapter
        val bottomRecyclerView =
            findViewById<RecyclerView>(R.id.bottomSheetRecycler)
        bottomRecyclerView.layoutManager =
            LinearLayoutManager(this@MainActivity)
        BottomSheetAdapter = BottomSheetAdapter(userCURListforAdapter)

        //move and swipe
        val swipeRecyclerCallback = object :SwipeRecyclerCallback(BottomSheetAdapter) {
            override fun isItemViewSwipeEnabled(): Boolean {
                return swipeEnabled
            }

            override fun isLongPressDragEnabled(): Boolean {
                return swipeEnabled
            }
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val startPosition = viewHolder.adapterPosition
                val endPosition = target.adapterPosition
                Collections.swap(userCURList, startPosition, endPosition)
                Collections.swap(userCURListforAdapter, startPosition, endPosition)
                GlobalScope.launch { curDao?.insertAll(userCURList) }
                return super.onMove(recyclerView, viewHolder, target)
            }
            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                val symbolForDelete = userCURList[viewHolder.adapterPosition]
                GlobalScope.launch { curDao?.deleteBySymbol(symbolForDelete) }
                userCURList.remove(symbolForDelete)
                userCURListforAdapter.removeAt(viewHolder.adapterPosition)
                super.onSwiped(viewHolder, direction)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeRecyclerCallback)
        itemTouchHelper.attachToRecyclerView(bottomRecyclerView)

        bottomRecyclerView.adapter = BottomSheetAdapter

// настройка колбэков при изменениях
        var bottomSheetReady = false
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (binding.btAdd.visibility == View.GONE) binding.btAdd.visibility = View.VISIBLE
                if (slideOffset >= 0) binding.btAdd.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset)
                    .setDuration(0).start()
                else {
                    binding.btAdd.animate().scaleX(1 + slideOffset).scaleY(1 + slideOffset).setDuration(0).start()
                }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        bottomSheetReady = false
                        tv_error.visibility = View.GONE
                        ivLock.animate().scaleX(0f).scaleY(0f).setDuration(300).start()
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        binding.btAdd.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
                        val fabAnim = binding.btAdd.getDrawable() as AnimationDrawable
                        fabAnim.start()
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        bottomSheetReady = true
                        binding.btAdd.visibility = View.GONE
                        ivLock.visibility = View.VISIBLE
//                        ivLock.animate().scaleX(0f).scaleY(0f).setDuration(0).start()
                        if (!swipeEnabled) ivLock.animate().alpha(0.5f).setDuration(300).start()
                        ivLock.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                        ivLock.animate().scaleX(0f).scaleY(0f).setDuration(300).start()
                        if (!bottomSheetReady) {
                            lifecycleScope.launch {
                                try {
                                    //get userList
                                    val getListFromDB = async {
                                        if (userCURList.isNullOrEmpty()) {
                                            val dblist = curDao?.getAll()
                                            dblist?.forEach {
                                                if (it != null) userCURList.add(it.CURsymbol)
                                            }
                                        }
                                    }
                                    if (CURList.isNullOrEmpty() || (dateCURupdate.time < System.currentTimeMillis() - 60000)) {
                                        if (settings["CoinMarketCupToken"].isNullOrEmpty()) throw Exception("No CoinMarketCupToken")
                                        CURList.clear()
                                        responce = get_data()
                                        info = JSONObject(responce)
                                        if (info.getJSONObject("status")
                                                .getInt("error_code") != 0
                                        ) {
                                            throw Exception(
                                                info.getJSONObject("status")
                                                    .getString("error_message")
                                            )
                                        } else {
                                            //update date
                                            val timeString =
                                                JSONObject(responce).getJSONObject("status")
                                                    .getString("timestamp")
                                            dateCURupdate =
                                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(
                                                    timeString
                                                )

                                            //create CURList
                                            val data = info.getJSONArray("data")
                                            NamesAndTickers.clear()
                                            withContext(Dispatchers.Default) {
                                                for (i in 0 until data.length() - 1) {
                                                    var tags = ""
                                                    val tagsArray = data.getJSONObject(i).getJSONArray("tags")
                                                    run breaking@{
                                                        for (i in 0 until tagsArray.length() - 1) {
                                                            tags += tagsArray[i].toString() + ", "
                                                            if (i > 4) {
                                                                tags.dropLast(1)
                                                                tags += "..."
                                                                return@breaking
                                                            }
                                                        }
                                                    }
                                                    if (tagsArray.length() < 6) tags = tags.removeSuffix(", ")
                                                    var oneCUR = CUR(
                                                        name = data.getJSONObject(i)
                                                            .getString("name"),
                                                        symbol = data.getJSONObject(i)
                                                            .getString("symbol"),
                                                        price = data.getJSONObject(i)
                                                            .getJSONObject("quote")
                                                            .getJSONObject("USD")
                                                            .getDouble("price"),
                                                        percent_change_1h = data.getJSONObject(i)
                                                            .getJSONObject("quote")
                                                            .getJSONObject("USD")
                                                            .getDouble("percent_change_1h"),
                                                        percent_change_24h = data.getJSONObject(i)
                                                            .getJSONObject("quote")
                                                            .getJSONObject("USD")
                                                            .getDouble("percent_change_24h"),
                                                        market_cap = data.getJSONObject(i)
                                                            .getJSONObject("quote")
                                                            .getJSONObject("USD")
                                                            .getDouble("market_cap"),
                                                        total_supply = data.getJSONObject(i)
                                                            .getString("total_supply"),
                                                        max_supply = data.getJSONObject(i)
                                                            .getString("max_supply"),
                                                        circulating_supply = data.getJSONObject(i)
                                                            .getString("circulating_supply"),
                                                        volume_24h = data.getJSONObject(i)
                                                            .getJSONObject("quote")
                                                            .getJSONObject("USD")
                                                            .getDouble("volume_24h"),
                                                        volume_change_24h = data.getJSONObject(i)
                                                            .getJSONObject("quote")
                                                            .getJSONObject("USD")
                                                            .getDouble("volume_change_24h"),
                                                        cmc_rank = data.getJSONObject(i)
                                                            .getInt("cmc_rank"),
                                                        tags =tags
                                                    )
                                                    CURList.add(oneCUR)
                                                    //create name for autocompletetextview
                                                    NamesAndTickers.add(
                                                        "${
                                                            data.getJSONObject(i)
                                                                .getString("symbol")
                                                        } | ${
                                                            data.getJSONObject(i).getString("name")
                                                        }" +
                                                                " | ${
                                                                    round(
                                                                        (data.getJSONObject(i)
                                                                            .getJSONObject("quote")
                                                                            .getJSONObject("USD")
                                                                            .getDouble("price")) * 100
                                                                    ) / 100
                                                                }$" +
                                                                " | ${
                                                                    round(
                                                                        (data.getJSONObject(i)
                                                                            .getJSONObject("quote")
                                                                            .getJSONObject("USD")
                                                                            .getDouble("percent_change_24h")) * 100
                                                                    ) / 100
                                                                }%"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    //create user CURList
                                    getListFromDB.await()
                                    if (userCURList.isNullOrEmpty()) {
                                        for (i in 0 until 9) {
                                            if (!userCURListforAdapter.contains(CURList[i])) {
                                                GlobalScope.launch { curDao?.insert(CUREntity(CURsymbol = CURList[i].symbol)) }
                                                userCURList.add(CURList[i].symbol)
                                                userCURListforAdapter.add(CURList[i])
                                            }
                                        }
                                    } else {
                                        userCURListforAdapter.clear()
                                        userCURList.forEach { userSymbol ->
                                            val cur = CURList.find { it.symbol == userSymbol }
                                            if (cur != null) userCURListforAdapter.add(
                                                cur
                                            )
                                        }
                                    }
                                    //refresh RecyclerAdapter
                                    BottomSheetAdapter.notifyDataSetChanged()
                                    //refresh ACTV
                                    adapterACTV.notifyDataSetChanged()

                                    bottomSheetReady = true
                                } catch (e: Exception) {
                                    tv_error.text = e.toString()
                                    tv_error.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    suspend private fun get_data(): String? =
    withContext(Dispatchers.IO) {
        val url: HttpUrl = HttpUrl.Builder()
            .scheme("https")
            .host("pro-api.coinmarketcap.com")
            .addPathSegment("v1")
            .addPathSegment("cryptocurrency")
            .addPathSegment("listings")
            .addPathSegment("latest")
            .addQueryParameter(
                "limit",
                "999"
            )
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accepts","application/json")
            .addHeader("X-CMC_PRO_API_KEY", settings["CoinMarketCupToken"]!!)
            .build()
        val resp = client.newCall(request).execute()
        return@withContext resp.body?.string()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable("CURList", CURList)
        outState.putSerializable("dateCURupdate", dateCURupdate)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        CURList = savedInstanceState.getSerializable("CURList") as ArrayList<CUR>
        dateCURupdate = savedInstanceState.getSerializable("dateCURupdate") as Date
        super.onRestoreInstanceState(savedInstanceState)
    }

}
