package com.example.cmp354_project_kotlin

data class User(val username:String,val userId:Long, val userKey:String):Filterable{
    override fun contains(string: CharSequence): Boolean {
        return username.contains(string)
    }

    override fun getId(): Long {
        return userId
    }

    override fun toString(): String {
        return username
    }

}
