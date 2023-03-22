package com.like.ble.central.util

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.central.connect.executor.AbstractConnectExecutor
import com.like.ble.central.connect.result.ConnectResult
import com.like.ble.central.scan.executor.AbstractScanExecutor
import com.like.ble.central.scan.executor.ScanExecutor
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionDeviceDisconnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 中心设备蓝牙扫描并连接执行者。
 */
class ScanAndConnectExecutor(
    private val activity: ComponentActivity,
    private val connectExecutor: AbstractConnectExecutor
) {
    private val scanExecutor: AbstractScanExecutor by lazy {
        ScanExecutor(activity)
    }
    private var needScan: Boolean = false
    private var scanJob: Job? = null

    init {
        activity.lifecycleScope.launch {
            connectExecutor.connectFlow.collectLatest {
                when (it) {
                    is ConnectResult.Ready -> {
                    }
                    is ConnectResult.Error -> {
                        when (val exception = it.throwable) {
                            is BleExceptionBusy -> {
                            }
                            else -> {
                                needScan = exception is BleExceptionDeviceDisconnected
                            }
                        }
                    }
                    is ConnectResult.Result -> {
                        needScan = false
                    }
                }
            }
        }
    }

    suspend fun connect(timeout: Long = 10000L) {
        if (needScan) {
            scanAndConnect(timeout)
        } else {
            connectExecutor.connect(timeout)
        }
    }

    private suspend fun scanAndConnect(timeout: Long) = coroutineScope {
        scanJob = launch(Dispatchers.IO) {
            scanExecutor.scanFlow.collect {
                when (it) {
                    is ScanResult.Ready -> {
                        Log.v("TAG", "Ready")
                    }
                    is ScanResult.Success -> {
                        Log.d("TAG", "Success")
                    }
                    is ScanResult.Completed -> {
                        Log.i("TAG", "Completed")
                        scanJob?.cancel()
                    }
                    is ScanResult.Error -> {
                        Log.e("TAG", "Error ${it.throwable.message}")
                        when (val e = it.throwable) {
                            is BleExceptionBusy -> {
                            }
                            else -> {
                                scanJob?.cancel()
                            }
                        }
                    }
                    is ScanResult.Result -> {
                        Log.w("TAG", "Result ${it.device.address}")
                        if (connectExecutor.address == it.device.address) {
                            Log.w("TAG", "Result 找到设备，准备连接")
                            connectExecutor.connect(timeout = timeout)
                            scanJob?.cancel()
                        }
                    }
                }
            }
        }
        launch(Dispatchers.IO) {
            scanExecutor.startScan()
        }
    }

}
