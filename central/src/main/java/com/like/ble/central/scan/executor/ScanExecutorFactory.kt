package com.like.ble.central.scan.executor

import android.annotation.SuppressLint
import android.content.Context

object ScanExecutorFactory {
    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var scanExecutor: AbstractScanExecutor? = null

    fun get(context: Context): AbstractScanExecutor =
        scanExecutor ?: synchronized(this) {
            scanExecutor ?: ScanExecutor(context.applicationContext).apply {
                scanExecutor = this
            }
        }

}
