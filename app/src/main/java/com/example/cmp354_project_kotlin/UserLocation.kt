package com.example.cmp354_project_kotlin

import android.location.Location
import java.text.SimpleDateFormat
import java.util.*

data class UserLocation(val userId:Long=0,val latitude:Double=0.0,val longitude:Double=0.0,val time:Long=0, val speed:Float=0.0f ){


    companion object{
        fun formatDate(time:Long):String{
            return FORMATTER.format(Date(time))
        }
        private val FORMATTER=SimpleDateFormat("EEEE hh:mm")
    }
}