package com.example.cmp354_project_kotlin

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.cmp354_project_kotlin.databinding.ActivityMapsBinding
import com.example.salispos.adapters.MySpinnerAdapter
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.Marker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import gr.escsoft.michaelprimez.searchablespinner.interfaces.OnItemSelectedListener
import kotlin.random.Random

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,OnItemSelectedListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var db: FirebaseDatabase
    private lateinit var localDb: DatabaseConnector
    private lateinit var adapter: MySpinnerAdapter<User>
    private var resume=false
    var users = mutableListOf<User>()
    var liveLocations = mutableListOf<UserLocation>()
    private var updateLocationTask:liveLocationsUpdateTask?=null
    private var updateUsersTask:UsersUpdateTask?=null
    private val locationsListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d(TAG + "Locations", "new update")
            updateLocationTask= liveLocationsUpdateTask()
            updateLocationTask!!.execute(snapshot)

        }

        override fun onCancelled(error: DatabaseError) {
            showError("Failed to update locations, error : $error")
            Log.d(TAG, "Locations' OnCancelled called with error : $error")
        }
    }

    val usersListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d(TAG + "Users", "new update")
            updateUsersTask=UsersUpdateTask()
            updateUsersTask!!.execute(snapshot)
        }

        override fun onCancelled(error: DatabaseError) {
            showError("Failed to update users, error : $error")
            Log.d(TAG + "Users", "Users' OnCancelled called with error : $error")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val service = Intent(this, MyService::class.java)
        startService(service)
        // generateTestData()
        db = FirebaseDatabase.getInstance()
        localDb = DatabaseConnector(this)
        localDb.open()
        updateUsers(localDb.allUsers)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)



        binding.usersSpn.setAdapter(adapter)

        binding.usersSpn.setOnItemSelectedListener(this)
        binding.usersSpn.setSelectedItem(1)
        binding.deleteBtn.setOnClickListener {
            AlertDialog.Builder(this).setTitle("Confirmation").setMessage("Are you sure to delete all local locations for ${adapter.getItem(binding.usersSpn.selectedPosition)?.username} ?")
                .setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
                    val id = adapter.getItemId(binding.usersSpn.selectedPosition)
                    val res=localDb.deleteLocations(id)
                    updateMap(id)
                    showMessage("$res locations deleted successfully!")
                }
                .setNegativeButton("No",null)
                .show()

        }
        //----
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                123
            )
            Log.d(MyService.TAG, "Permission not  granted")
        }
        if (PreferenceManager.getDefaultSharedPreferences(this).getString("username","").isNullOrBlank())
        {
            intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        db.reference.child("Locations").addValueEventListener(locationsListener)
        db.reference.child("Users").addValueEventListener(usersListener)
        resume=true
    }

    override fun onDestroy() {
        localDb.close()
        super.onDestroy()
    }
    override fun onPause() {
        resume=false
        db.reference.child("Locations").removeEventListener(locationsListener)
        db.reference.child("Users").removeEventListener(usersListener)
        updateLocationTask?.cancel(true)
        updateUsersTask?.cancel(true)
        super.onPause()

    }

    fun drawMarkers(list: List<UserLocation>) {
        if (this::mMap.isInitialized) {
            mMap.clear()
            list.forEach {
                mMap.addMarker(
                    MarkerOptions().position(LatLng(it.latitude, it.longitude)).title(
                        "${(users.find { u -> u.userId == it.userId }?.username) ?: ""} | ${
                            UserLocation.formatDate(it.time)
                        } | Speed = ${it.speed}"
                    )
                )
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
       // val sydney = LatLng(-34.0, 151.0)
       // mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
       // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        mMap.setInfoWindowAdapter(object : InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View? {
                val v: View = layoutInflater.inflate(R.layout.marker_layout, null)
                v.layoutParams =
                    RelativeLayout.LayoutParams(800, RelativeLayout.LayoutParams.WRAP_CONTENT)
                val tv = v.findViewById<View>(R.id.textView) as TextView
                tv.text = marker.title
                return v
            }
        })
    }

    private fun generateTestData() {
        val local_db = DatabaseConnector(this)
        val id = local_db.saveUser("test4", "test4_key")
        Log.d("User saved!", "ID= $id")
        Log.d("GetUsers!", "${local_db.allUsers}")
        val rand = Random(1)
        local_db.allUsers.forEach {
            for (i in 1..10)
                local_db.insertLocation(
                    UserLocation(
                        userId = it.userId,
                        rand.nextDouble(20.0000, 26.0000),
                        rand.nextDouble(until = 39.38, from = 23.1688),
                        System.currentTimeMillis(),
                        rand.nextFloat()
                    )
                )
        }
        //21.294459, 39.383807 ,23.1688,58.20
        //20, 26

        Log.d("GetLastLocations!", "${local_db.lastLocations}")
        Log.d("local_db.getLastTime(7)!", "${local_db.getLastTime(7)}")
        Log.d("getID!", "${local_db.getUserId("test4_key")}")
        Log.d("getID!", "${local_db.getUserId("test4_key_")}")
        // local_db.deleteLocations(6)
        // local_db.deleteAllLocations()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.settings_menu -> {
                // start settings activity
                intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.deleteall_menu ->
            {
                AlertDialog.Builder(this).setTitle("Confirmation").setMessage("Are you sure you want to delete all local locations for all users?")
                    .setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
                        val res= localDb.deleteAllLocations()
                       binding.usersSpn.setSelectedItem(1)
                        updateMap(-1)
                        showMessage("$res locations deleted successfully!")
                    }
                    .setNegativeButton("No",null)
                    .show()
                true
            }

            else -> true

        }
    }

    companion object {
        const val TAG = "MapsActivity!"
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    }

    private inner class liveLocationsUpdateTask :
        AsyncTask<DataSnapshot, Unit, List<UserLocation>>() {
        override fun doInBackground(vararg p0: DataSnapshot?): List<UserLocation> {
            var list = mutableListOf<UserLocation>()
            val snapshot = p0[0]
            snapshot?.children?.forEach { ds ->
                ds.getValue<MyLocation>()?.let { loc ->
                    ds.key?.let { key ->
                        users.find { it.userKey == key }?.let { u ->
                            val ul = UserLocation(
                                u.userId,
                                loc.latitude,
                                loc.longitude,
                                loc.time,
                                loc.speed
                            )
                            localDb.insertLocation(ul)
                            Log.d(TAG,"Location added : $ul")
                            list.add(ul)
                        }
                    }

                }
            }

            return list

        }

        override fun onPostExecute(result: List<UserLocation>) {
            liveLocations.clear()
            liveLocations.addAll(result)
            updateMap(adapter.getItemId(binding.usersSpn.selectedPosition))
            showMessage("Locations updated successfully")

        }
    }

    private inner class UsersUpdateTask : AsyncTask<DataSnapshot, Unit, List<User>>() {
        override fun doInBackground(vararg p0: DataSnapshot?): List<User> {

            val snapshot = p0[0]
            val keys= mutableListOf<String>()
            snapshot?.children?.forEach { ds ->
                ds.getValue<String>()?.let { u ->
                    ds.key?.let { key ->
                        Log.d(TAG,"User being update = $key : $u ")
                        localDb.saveUser(username = u, userKey = key)
                        keys.add(key)
                    }
                }
            }

            val users=localDb.allUsers
            users.forEach {u->
                if (keys.find{u.userKey==it} ==null){
                    localDb.deleteLocations(u.userId)
                    localDb.deleteUser(u.userId)
                }
            }

            return localDb.allUsers
        }

        override fun onPostExecute(result: List<User>) {
            updateUsers(result)

            showMessage("Users updated successfully ")

        }
    }

    private fun updateUsers(list: List<User>) {
        users.clear()
        users.add(User("Realtime locations for all", -1, ""))
        users.addAll(list)
        //Log.d(TAG+"users"," after update $users")
        adapter=MySpinnerAdapter(this,users)
        binding.usersSpn.setAdapter(adapter)

    }

    private fun updateMap(id: Long){

        if (id!=(-1).toLong())
        {
            binding.deleteBtn.visibility=View.VISIBLE
            if (resume){
                val list = localDb.getUserLocations(id)
                drawMarkers(localDb.getUserLocations(id))
                Log.d(TAG, "Markers : ${list.size}")
                showMessage("${list.size} locations found")
            }
            }

        else
        {
            binding.deleteBtn.visibility=View.GONE
            drawMarkers(liveLocations)
        }
    }

    override fun onItemSelected(view: View?, position: Int, id: Long) {
        Log.d(TAG, "ItemSelected id= ${adapter.getItemId(position)} ,pos= $position")
      updateMap(  adapter.getItemId(position))
}
    override fun onNothingSelected() {
       // TODO("Not yet implemented")

    }

}