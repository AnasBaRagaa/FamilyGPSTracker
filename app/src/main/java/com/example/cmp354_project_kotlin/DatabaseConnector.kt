package com.example.cmp354_project_kotlin

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.core.database.getLongOrNull
import java.util.ArrayList


class DatabaseConnector(context: Context) {
    private var database: SQLiteDatabase? = null
    private val databaseOpenHelper: DatabaseOpenHelper

    // open the database connection
    @Throws(SQLException::class)
    fun open() {
        // create or open a database for reading/writing
        database = databaseOpenHelper.writableDatabase //TS: at the first call, onCreate is called
    }

    fun close() {
        if (database != null) database!!.close()
    }

    fun insertLocation(ul: UserLocation) {
       // open() // open the database
        val last=getLastTime_(ul.userId)
        if (ul.time>last.time &&( ul.latitude != last.latitude || ul.longitude!= last.longitude ) ){
            val newContact: ContentValues = ContentValues()
            newContact.put(LATITUDE, ul.latitude)
            newContact.put(LONGITUDE, ul.longitude)
            newContact.put(SPEED, ul.speed)
            newContact.put(TIME, ul.time)
            newContact.put(USER_ID, ul.userId)


            database!!.insert(LOCATION_TABLE, null, newContact)
        }
         //   close() // close the database
    }

    fun saveUser(username:String, userKey:String):Long{
        //This method insert new user or update the username if the user exists
        val newContact: ContentValues = ContentValues()
        newContact.put(USERNAME, username)
        newContact.put(USER_KEY, userKey)

      //  open() // open the database
        val rs=database!!.rawQuery("select _id from $USER_TABLE where $USER_KEY = ?", arrayOf(userKey))
        var id:Long?=null

        if( rs.moveToFirst())
            id=rs.getLongOrNull(0)
        rs.close()
        if(id==null)
        {
         id=database!!.insertWithOnConflict(USER_TABLE,null,newContact, CONFLICT_REPLACE)
        }
        else {
            database!!.updateWithOnConflict(USER_TABLE, contentValuesOf(Pair(USERNAME,username)),"_id = ?",
                arrayOf(id.toString()),CONFLICT_REPLACE)
        }
      //  close() // close the database
        return id
    }




    // return a Cursor with all contact information in the database
    val allLocations: List<UserLocation>
        get() {
            val locations= mutableListOf<UserLocation>()
        //    open()
            var rs=database!!.rawQuery("SELECT * from $LOCATION_TABLE ;",null)
            if (rs.moveToFirst())
                do {
                    locations+= UserLocation(userId =rs.getLong(0),latitude = rs.getDouble(1)  ,longitude= rs.getDouble(2),speed = rs.getFloat(3),time = rs.getLong(4))
                }
                while (rs.moveToNext())
            rs.close()
         //   close()
            return locations.toList()
        }

    fun getUserLocations(id:Long):List<UserLocation>{
        val locations= mutableListOf<UserLocation>()
      //  open()
        var rs=database!!.rawQuery("SELECT * from $LOCATION_TABLE l where l.userId= $id  ;",null)
        if (rs.moveToFirst())
            do {
                locations+= UserLocation(userId =rs.getLong(4),latitude = rs.getDouble(1)  ,longitude= rs.getDouble(2),speed = rs.getFloat(3),time = rs.getLong(5))
            }
            while (rs.moveToNext())
        rs.close()
      //  close()
        return locations.toList()
    }

    fun deleteLocations(id: Long):Int {
        //this function deletes all locations for a specific user
     //   open()
        val res=database!!.delete(LOCATION_TABLE, "$USER_ID=$id", null)
        Log.d(TAG,"$res locations deleted for user $id")
       // close()
        return if(res>0) res else 0
    }

    fun deleteUser(id: Long) {
        //this function deletes all locations for a specific user
        //   open()
        val res=database!!.delete(USER_TABLE, "_id=$id", null)
        Log.d(TAG,"$res user with $id deleted")
        // close()

    }
    fun deleteAllLocations():Int {
        // this function deletes all locations
      //  open()
        val res=database!!.delete(LOCATION_TABLE, null, null)
        Log.d(TAG,"all $res locations deleted")
      //  close()
        return if(res>0) res else 0

    }
    fun getUserId(key:String):Long?{
        var id:Long?=null
      //  open()
        var rs=database!!.rawQuery("SELECT _id from $USER_TABLE where $USER_KEY = ?;", arrayOf(key))
        if (rs.moveToFirst())
            id=rs.getLongOrNull(0)
        rs.close()
      //  close()
        return id
    }



    //This attribute returns a list of users in the local database
    val allUsers: List<User>
        //This value returns a list of all users in the local database
        get() {

            //This function returns a list of users in the local database
            val users= mutableListOf<User>()
         //   open()
            //var rs=database!!.rawQuery("SELECT * from $USER_TABLE u where (select count(*) from $LOCATION_TABLE where $USER_ID=u._id)>0",null)
            var rs=database!!.rawQuery("select * from $USER_TABLE ;",null)
            if (rs.moveToFirst())
                do {
                    users+= User(userId =rs.getLong(0),userKey =rs.getString(1)  ,username= rs.getString(2))
                }
                    while (rs.moveToNext())
                    rs.close()
          //          close()
            return users.toList()
        }

    val allActiveUsers: List<User>
        //This value returns a list of all users in the local database who have location records
        get() {
            //This function returns a list of users in the local database
            val users= mutableListOf<User>()
          //  open()
            var rs=database!!.rawQuery("SELECT * from $USER_TABLE u where (select count(*) from $LOCATION_TABLE where $USER_ID=u._id)>0",null)
            if (rs.moveToFirst())
                do {
                    users+= User(userId =rs.getLong(0),userKey =rs.getString(1)  ,username= rs.getString(2))
                }
                while (rs.moveToNext())
            rs.close()
         //   close()
            return users.toList()
        }


    fun getLastTime(id: Long): Long {
        // this function returns the last update for a given user id
        var time:Long?=0
        //open()
        var rs=database!!.rawQuery("SELECT Time from locations where userID=? ORDER by time DESC LIMIT 1;", arrayOf(id.toString()))
        if (rs.moveToNext())
            time=rs.getLongOrNull(0)
       // close()
        //TODO
        return time?:0
    }

    private fun getLastTime_(id: Long): MyLocation {
        // this function returns the last update for a given user id
        var time:Long?=0
        var loc=MyLocation()

        var rs=database!!.rawQuery("SELECT $TIME,$LATITUDE,$LONGITUDE,$SPEED from locations where userID=? ORDER by time DESC LIMIT 1;", arrayOf(id.toString()))
        if (rs.moveToNext())
            loc= MyLocation(latitude = rs.getDouble(1),longitude = rs.getDouble(2),speed = rs.getFloat(3),time=rs.getLong(0))

        return loc
    }

    // this function returns the last location for every user
    //TODO
    val lastLocations: List<UserLocation>
        get() {
            // this function returns the last known location from every user in the local database
            val locations= mutableListOf<UserLocation>()
        //    open()
            var rs=database!!.rawQuery("SELECT $USER_ID,$LATITUDE,$LONGITUDE,$SPEED,max(time) as $TIME from locations GROUP by $USER_ID  ;",null)
            if (rs.moveToFirst())
                do {
                    locations+= UserLocation(userId =rs.getLong(0),latitude = rs.getDouble(1)  ,longitude= rs.getDouble(2),speed = rs.getFloat(3),time = rs.getLong(4))
                }
                while (rs.moveToNext())
            rs.close()
         //   close()
            return locations.toList()
        }

    private inner class DatabaseOpenHelper  // public constructor
        (
        context: Context?, name: String?,
        factory: CursorFactory?, version: Int
    ) :
        SQLiteOpenHelper(context, name, factory, version) {
        // creates the contacts table when the database is created
        // TS: this is called from  open()->getWritableDatabase(). Only if the database does not exist
        override fun onCreate(db: SQLiteDatabase) {
            // query to create a new table named contacts
            val createLocationsQuery: String = ("CREATE TABLE $LOCATION_TABLE" +
                    "(_id integer primary key autoincrement," +
                    "$LATITUDE REAL, $LONGITUDE REAL, $SPEED REAL, $USER_ID integer," +
                    "$TIME integer);")
            val createUserQuery:String = ("CREATE TABLE $USER_TABLE" +
                    "(_id integer primary key autoincrement," +
                    " $USER_KEY TEXT UNIQUE NOT NULL, $USERNAME TEXT);" )

            db.execSQL(createLocationsQuery) // execute the query
            db.execSQL(createUserQuery)
        }

        override fun onUpgrade(
            db: SQLiteDatabase, oldVersion: Int,
            newVersion: Int
        ) {
        }
    }

    companion object {
        private val DATABASE_NAME: String = "local_location.db"
        val LOCATION_TABLE: String = "locations"
        val LATITUDE: String = "latitude"
        val LONGITUDE: String = "longitude"
        val TIME: String = "time"
        val SPEED: String = "speed"
        val USER_KEY: String = "userKey"
        val USER_TABLE="Users"
        val USERNAME="username"
        val USER_ID="userID"
        val TAG="DatabaseConnector!"
    }

    init {
        databaseOpenHelper = DatabaseOpenHelper(context, DATABASE_NAME, null, 1)
    }
}