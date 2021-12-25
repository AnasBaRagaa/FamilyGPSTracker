package com.example.cmp354_project_kotlin

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import java.util.*
import android.location.LocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.graphics.Color
import android.widget.Toast
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class MyService : Service() , SharedPreferences.OnSharedPreferenceChangeListener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {
     var timer:Timer?=null
    var minutes:Int=1
    private lateinit var  googleApiClient: GoogleApiClient
    private val locationRequest: LocationRequest? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firebase:FirebaseDatabase

    lateinit var preferences: SharedPreferences
    override fun onCreate() {
        Log.d(TAG, "Service created")
        googleApiClient= GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener (this).build()
        preferences= PreferenceManager.getDefaultSharedPreferences(this)
        minutes=preferences.getInt("sync",1)
        firebase= FirebaseDatabase.getInstance()

    }
    private fun createNotificationChannel(): String{
        //taken from https://stackoverflow.com/questions/6397754/android-implementing-startforeground-for-a-service
        val channelId = "my_service"
        val channelName = "My Background Service"
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.BLUE
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        if (intent?.getBooleanExtra("foreground",false) == true) {
            val notificationBuilder = NotificationCompat.Builder(this, createNotificationChannel() )
            val notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
            startForeground(1100,notification)
        }
        preferences.registerOnSharedPreferenceChangeListener(this)

        googleApiClient.connect()
        startTimer()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {

        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        stopTimer()
        googleApiClient.disconnect()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        Toast.makeText(this, "The location service stopped running", Toast.LENGTH_SHORT).show();

    }

    private fun startTimer() {
        Log.d(TAG,"Timer Starting")
        val task: TimerTask = object : TimerTask() {
            override fun run() {
                // Read GPS location and send data to the user
                if (ActivityCompat.checkSelfPermission(
                        this@MyService,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@MyService,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    Log.d(MyService.TAG, "Permission not  granted")
                    return
                }



                    val location =
                        LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
                    location?.let {
                        var key = preferences.getString("userKey", "")
                        if (!key.isNullOrBlank()) {
                            firebase.reference.child("Locations").child(key).setValue(
                                MyLocation(
                                    latitude = it.latitude,
                                    longitude = it.longitude,
                                    speed = it.speed,
                                    time = System.currentTimeMillis()//it.time
                                )
                            )
                        }

                    Log.d(TAG,"LOCATION Pushed")
                    }
                Log.d(TAG," $location")


                    Log.d(TAG,"Timer called")

            }
        }
        timer?.cancel()
        timer = Timer(true)
        val interval= if (preferences.getBoolean("test",false)) (minutes *1000).toLong() else (minutes *1000*60).toLong()

        timer!!.schedule(task, 1,
         //   (minutes *60*1000).toLong()
            interval
        )
    }

    private fun stopTimer() {
        Log.d(TAG,"Timer stopped")
            timer?.cancel()

    }
    companion object{
        const val TAG="MyService!"
        const  private val CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        Log.d(TAG+"SP","Shared preferences changed, key= $key")
        if (key=="sync" || key=="test"){
            minutes=preferences.getInt("sync",1)
            Log.d(TAG+"SP","Shared preferences changed, MINUTES= $minutes")
            stopTimer()
            startTimer()
        }


    }

    override fun onConnected(p0: Bundle?) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                123
//            )
            Log.d(TAG,"Permission not  granted")
        }
        else {
            val location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)

            //Log.d(TAG, "Location init  : " + location.latitude + " & " + location.longitude)
        }


//        LocationServices.FusedLocationApi.requestLocationUpdates(
//            googleApiClient, locationRequest, this
//        )


    }

    override fun onConnectionSuspended(p0: Int) {
       // TODO("Not yet implemented")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
            AlertDialog.Builder(this)
                .setMessage("Connection failed. Error code: ${connectionResult.getErrorCode()} ")
                .show()
        Log.e(TAG,"onConnectionFailed is called with error code ${connectionResult.getErrorCode()}")
    }

    override fun onLocationChanged(p0: Location) {
       // TODO("Not yet implemented")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val intent= Intent(this,MyReceiver::class.java)
        intent.setAction("restartService")
        sendBroadcast(intent)
        super.onTaskRemoved(rootIntent)
    }

}