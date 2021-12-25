package com.example.cmp354_project_kotlin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.d("MyReceiver!","called!")
       val intent=Intent(context,MyService::class.java)
        intent.putExtra("foreground",true)
      context.startForegroundService(intent)
    }
}