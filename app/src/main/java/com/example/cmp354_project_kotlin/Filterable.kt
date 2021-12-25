package com.example.cmp354_project_kotlin

interface Filterable {
    fun contains(string: CharSequence):Boolean
    fun getId():Long

}