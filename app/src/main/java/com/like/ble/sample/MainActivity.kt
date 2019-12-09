package com.like.ble.sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun gotoBleActivity(view: View) {
        this.startActivity(Intent(this, BleActivity::class.java))
    }

    fun gotoBlePeripheralActivity(view: View) {
        this.startActivity(Intent(this, BlePeripheralActivity::class.java))
    }

}
