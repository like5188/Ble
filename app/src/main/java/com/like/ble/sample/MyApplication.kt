package com.like.ble.sample

import android.app.Application
import com.starcaretech.stardata.StarData

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        StarData.init()
    }
}