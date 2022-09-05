package com.like.ble.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.like.common.util.startActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun gotoBleActivity(view: View) {
        startActivity<BleCentralActivity>()
    }

    fun gotoBlePeripheralActivity(view: View) {
        startActivity<BlePeripheralActivity>()
    }

}
