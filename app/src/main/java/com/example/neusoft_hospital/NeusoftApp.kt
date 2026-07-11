package com.example.neusoft_hospital

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NeusoftApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}