package com.bds89.miner2android

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bds89.miner2android.forRoom.App
import com.bds89.miner2android.forRoom.AppDatabase
import com.bds89.miner2android.forRoom.PCsEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.*


class AllNotification(context: Context, workerParams: WorkerParameters) : Worker(context,
    workerParams
) {
    val dir: File = applicationContext.filesDir
    var PCList_load: ArrayList<PC> = arrayListOf()
    var last_resonce_time = hashMapOf<String, Int>()
    var all_notification = hashMapOf<String, MutableMap<String, String>>()
    var wasNewNotifications = false
    var silentNotifications = true
    //DB
    val db: AppDatabase = App.instance.database
    val PCsDao = db.pcsDao()

    private fun sendNotify(all:MutableMap<String, MutableMap<String, String>>) {
        var id_for_Pending = 0
        all.forEach { title, value->
            var position = 99999
            var id = 99999
            //find position of PC
            PCList_load.forEachIndexed { index, PC ->
                if (PC.name == title) {
                    position = index
                    id = PC.id
                }
            }

            //create intent, for open nodeinfo
            val intent = Intent(applicationContext, NodeInfoActivity::class.java)
            intent.apply {
                putExtra(const.KEY_PCList, PCList_load)
                putExtra(const.KEY_PosNum, position)
                putExtra("from_notification", true)
                putExtra("title", title)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(applicationContext, id, intent, PendingIntent.FLAG_IMMUTABLE)

            //create intent for swipe to dismiss
            val dismissIntent = Intent(applicationContext, MyBroadcastReceiver::class.java).apply {
                putExtra("title", title)
                setAction("com.bds89.miner2android.swipeNotification")
            }
            val pendingDismissIntent = PendingIntent.getBroadcast(applicationContext, id_for_Pending, dismissIntent, PendingIntent.FLAG_IMMUTABLE)
            id_for_Pending += 1
            // Создаём уведомление
            var builder = NotificationCompat.Builder(applicationContext, const.CHANNEL_ID_LIMITS)
                .setSmallIcon(R.drawable.mining3)
                .setAutoCancel(true)
//            .setLargeIcon(
//                BitmapFactory.decodeResource(applicationContext.getResources(),
//                R.drawable.mining)) // большая картинка
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setShowWhen(true)
                .setDeleteIntent(pendingDismissIntent)
                .setSilent(silentNotifications)
            if (position != 99999) builder.setContentIntent(pendingIntent)

            var style = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
            var numberOfLines = 0
            value.forEach addingLines@ { textKey, textValue ->
                style.addLine(textKey+textValue)
                numberOfLines+=1
                if (numberOfLines > 4)  {
                    style.setSummaryText("+${(value.size-4)}")
                    return@addingLines
                }
                }
            builder.setStyle(style)

            with(NotificationManagerCompat.from(applicationContext)) {
                notify(id, builder.build()) // посылаем уведомление
            }
        }
    }

    private fun check_limits() {
        GlobalScope.launch {
            var ips = hashMapOf<String, MutableMap<String, String>>()
            //load PCList
            PCList_load = PCsEntity.listToPCList(PCsDao?.getAll())
            //try to load last_resonce_time
            try {
                val file = FileInputStream("$dir/${const.KEY_SaveLastResponce}")
                val inStream = ObjectInputStream(file)
                last_resonce_time = inStream.readObject() as HashMap<String, Int>
                inStream.close()
                file.close()
            } catch (e: Exception) {
            }
            //try to load all_notification
            try {
                val file = FileInputStream("$dir/${const.KEY_SaveAllNotification}")
                val inStream = ObjectInputStream(file)
                all_notification =
                    inStream.readObject() as HashMap<String, MutableMap<String, String>>
                inStream.close()
                file.close()
            } catch (e: Exception) {
            }
            if (!PCList_load.isNullOrEmpty()) {
                PCList_load.forEach { PC ->
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
                val client = OkHttpClient()
                var overload_limits = mutableMapOf<String, Any>()
                ips.forEach { ip ->
                    val url = "http://${ip.key}/control"
                    PCList_load.forEach findPC@{ pc ->
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
                                "value" to ip.value
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
                                        overload_limits.put(key, jsnData.get(key))
                                    }
                                }
                            } catch (e: java.lang.Exception) {
                            }
                            return@findPC
                        }
                    }
                }
                overload_limits.forEach { name, value ->
                    if (value is Int && (PCList_load.find { it.name == name }) != null) last_resonce_time.put(name, value)
                    else {
                        if (!(value as JSONObject).has("code") && (PCList_load.find { it.name == name }) != null) {
                            value.keys().forEach { gpu ->
                                last_resonce_time.put(
                                    name,
                                    (System.currentTimeMillis() / 1000).toInt()
                                )
                                val type = value.get(gpu) as JSONObject
                                (type).keys().forEach { param ->
                                    var textKey = ""
                                    var textValue = ""
                                    if (const.namesofparams.containsKey(gpu))
                                        textKey += const.namesofparams[gpu]?.let {
                                            applicationContext.getString(
                                                it
                                            )
                                        }
                                    else textKey += gpu
                                    if (const.namesofparams.containsKey(param)) {
                                        textKey += ". ${
                                            const.namesofparams[param]?.let {
                                                applicationContext.getString(
                                                    it
                                                )
                                            }
                                        }: "
                                        textValue = "${type.get(param)}"
                                    } else {
                                        textKey += ". $param: "
                                        textValue += "${type.get(param)}"
                                    }

                                    if (!textKey.isNullOrEmpty()) {
                                        if (all_notification.containsKey(name)) all_notification[name]?.put(
                                            textKey,
                                            textValue
                                        )
                                        else {
                                            all_notification.put(
                                                name,
                                                mutableMapOf(textKey to textValue)
                                            )
                                            silentNotifications = false
                                        }
                                        wasNewNotifications = true
                                    }
                                }
                            }
                        }
                    }
                }
                val temp = arrayListOf<String>()

                last_resonce_time.forEach { name, value ->
                    val t2 = System.currentTimeMillis() / 1000
                    val tdelta = t2 - value
                    val textTime = DateUtils.formatElapsedTime(tdelta)
                    if (tdelta > (31 * 60) && internetIsConnected()) {
                        if (all_notification.containsKey(name)) all_notification[name]?.put(
                            applicationContext.getString(R.string.no_responce),
                            textTime
                        )
                        else {
                            all_notification.put(
                                name,
                                mutableMapOf(applicationContext.getString(R.string.no_responce) to textTime)
                            )
                            silentNotifications = false
                        }
                        temp.add(name)
                        wasNewNotifications = true
                    }
                }
                temp.forEach { name -> last_resonce_time.remove(name) }
                //save last_resonce_time
                try {
                    val file = FileOutputStream("$dir/${const.KEY_SaveLastResponce}")
                    val outStream = ObjectOutputStream(file)
                    outStream.writeObject(last_resonce_time)
                    outStream.close()
                    file.close()

                } catch (e: Exception) {
                }
                //save all_notification
                try {
                    val file = FileOutputStream("$dir/${const.KEY_SaveAllNotification}")
                    val outStream = ObjectOutputStream(file)
                    outStream.writeObject(all_notification)
                    outStream.close()
                    file.close()

                } catch (e: Exception) {
                }
                if (wasNewNotifications) sendNotify(all_notification)
            }
        }
    }

    private fun internetIsConnected():Boolean {
        return try {
            val command = "ping -c 1 google.com"
            Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    override fun doWork(): Result {
        check_limits()
        return Result.success()
    }

}