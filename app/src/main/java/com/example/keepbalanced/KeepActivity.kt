package com.example.keepbalanced

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class KeepActivity : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}