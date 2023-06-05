package com.like.ble.central.util

import android.content.Context
import com.like.ble.central.connect.executor.ConnectExecutorFactory
import com.like.ble.central.scan.executor.ScanExecutorFactory
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionCancelTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectHelper {

    /**
     * 扫描并连接指定地址的蓝牙设备。
     *
     * @param addresses                 需要连接的蓝牙设备地址。
     * @param onDisconnectedListener    如果连接成功后再断开，就会触发此回调。
     * 注意：当断开原因为关闭蓝牙开关时，不回调，由 [BleBroadcastReceiverManager] 设置的监听来回调。
     * @throws [com.like.ble.exception.BleException]
     */
    suspend fun scanAndConnect(
        context: Context,
        timeout: Long = 20000L,
        addresses: List<String>,
        onDisconnectedListener: ((String, Throwable) -> Unit)? = null
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
                scanErrorToConnectErrorAndThrow(it)
            }
            .toList()
        val cost = System.currentTimeMillis() - startTime
        val remainTime = timeout - cost
        withContext(Dispatchers.IO) {
            scanAddresses.forEach { address ->
                launch {
                    ConnectExecutorFactory.get(context, address).connect(timeout = remainTime, onDisconnectedListener = {
                        onDisconnectedListener?.invoke(address, it)
                    })
                }
            }
        }
    }

    fun disconnect(context: Context) {
        try {
            ScanExecutorFactory.get(context).stopScan()
        } catch (e: BleException) {
            scanErrorToConnectErrorAndThrow(e)
        }
        ConnectExecutorFactory.getExecutors().forEach {
            it.disconnect()
        }
    }

    /**
     * 在连接时，不能报扫描相关的错误，需要转换成连接相关的错误。
     */
    private fun scanErrorToConnectErrorAndThrow(throwable: Throwable) {
        throw  when (throwable) {
            is BleExceptionCancelTimeout -> {
                throwable
            }
            else -> {
                BleException("连接蓝牙失败，未找到蓝牙设备")
            }
        }
    }

}
