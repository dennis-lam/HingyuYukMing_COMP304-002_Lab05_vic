package com.example.hingyuyukming_comp304_002_lab05

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LandmarkApplication: Application() {
    lateinit var landmarks: Landmarks
        private set

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            landmarks = Landmarks(applicationContext)
        }
    }
}