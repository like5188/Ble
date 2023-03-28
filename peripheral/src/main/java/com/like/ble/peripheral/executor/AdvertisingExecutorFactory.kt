package com.like.ble.peripheral.executor

import android.annotation.SuppressLint
import android.content.Context

object AdvertisingExecutorFactory {
    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var advertisingExecutor: AbstractAdvertisingExecutor? = null

    fun get(context: Context): AbstractAdvertisingExecutor =
        advertisingExecutor ?: synchronized(this) {
            advertisingExecutor ?: AdvertisingExecutor(context.applicationContext).apply {
                advertisingExecutor = this
            }
        }

}
