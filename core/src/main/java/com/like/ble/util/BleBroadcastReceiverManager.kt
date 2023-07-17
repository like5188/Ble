package com.like.ble.util

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BleBroadcastReceiverManager(private val mContext: Context, onBleOn: (() -> Unit)? = null, onBleOff: (() -> Unit)? = null) {
    // 蓝牙打开关闭监听器
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_ON -> {// 蓝牙已打开
                            onBleOn?.invoke()
                        }
                        BluetoothAdapter.STATE_OFF -> {// 蓝牙已关闭
                            onBleOff?.invoke()
                        }
                    }
                }
            }
        }
    }

    fun register() {
        mContext.registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    fun unregister() {
        try {
            mContext.unregisterReceiver(mReceiver)
        } catch (e: Exception) {// 避免 java.lang.IllegalArgumentException: Receiver not registered
        }
    }
}

