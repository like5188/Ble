package com.like.ble.central.util

import android.content.Context
import com.like.ble.central.connect.executor.ConnectExecutorFactory
import com.like.ble.central.scan.executor.ScanExecutorFactory
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionCancelTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectHelper {

    suspend fun scanAndConnect(
        context: Context,
        timeout: Long = 10000L,
        addresses: List<String>,
        onDisconnectedListener: ((Throwable) -> Unit)? = null
    ) {
        val startTime = System.currentTimeMillis()
        val addressesTemp = addresses.toMutableList()
        val scanAddresses = ScanExecutorFactory.get(context).startScan(timeout = timeout)
            .filter {
                addressesTemp.contains(it.device.address) && addressesTemp.remove(it.device.address)
            }
            .take(addresses.size)
            .map {
                it.device.address
            }
            .catch {
                // 在连接时，不能报扫描相关的错误，需要转换成连接相关的错误。
                throw  when (it) {
                    is BleExceptionCancelTimeout, is BleExceptionBusy -> {
                        it
                    }
                    else -> {
                        BleException("连接蓝牙失败，未找到蓝牙设备")
                    }
                }
            }
            .toList()
        val cost = System.currentTimeMillis() - startTime
        val remainTime = timeout - cost
        withContext(Dispatchers.IO) {
            scanAddresses.forEach {
                launch {
                    ConnectExecutorFactory.get(context, it).connect(timeout = remainTime, onDisconnectedListener = onDisconnectedListener)
                }
            }
        }
    }

}