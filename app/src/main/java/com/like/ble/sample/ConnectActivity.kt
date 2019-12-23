package com.like.ble.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class ConnectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: BleInfo? = intent.getSerializableExtra("data") as? BleInfo
        Log.e("ConnectActivity", "data=$data")
    }

}