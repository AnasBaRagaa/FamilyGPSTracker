package com.example.cmp354_project_kotlin

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.firebase.database.FirebaseDatabase

class App:Application() {
    private lateinit var preferences: SharedPreferences
    override fun onCreate() {
        super.onCreate()
        Log.d("App!", "App started")
        preferences=PreferenceManager.getDefaultSharedPreferences(this)
        if (preferences.getString("userKey","").isNullOrBlank()){
            val db=FirebaseDatabase.getInstance().reference
           val key= db.child("Users").push().key?: Settings.Secure.getString(getContentResolver(), "bluetooth_name");

            key?.let {
                val editor=preferences.edit()
                editor.putString("userKey",it)
                editor.apply()
                db.child("Users").child(it).setValue(preferences.getString("username",""))
            }
        }


    }
}