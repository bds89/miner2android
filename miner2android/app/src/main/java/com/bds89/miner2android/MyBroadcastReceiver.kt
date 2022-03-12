package com.bds89.miner2android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.*

class MyBroadcastReceiver : BroadcastReceiver() {
    var all_notification = hashMapOf<String, MutableMap<String, Int>>()
    var title = ""


    override fun onReceive(context: Context, intent: Intent?) {
        val dir: File = context.filesDir
        title = intent?.getStringExtra("title").toString()
        if (title != "") {
            //try to load all_notification
            try {
                val file = FileInputStream("$dir/${const.KEY_SaveAllNotification}")
                val inStream = ObjectInputStream(file)
                all_notification = inStream.readObject() as HashMap<String, MutableMap<String, Int>>
                inStream.close()
                file.close()
            } catch (e: Exception) {
            }
            all_notification.remove(title)
        }
        //save all_notification
        try {
            val file = FileOutputStream("$dir/${const.KEY_SaveAllNotification}")
            val outStream = ObjectOutputStream(file)
            outStream.writeObject(all_notification)
            outStream.close()
        } catch (e: Exception) { }
    }
}
