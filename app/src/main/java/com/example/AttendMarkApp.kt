package com.example

import android.app.Application

class AttendMarkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AttendMarkApp
            private set
    }
}
