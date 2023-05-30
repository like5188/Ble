package com.like.ble.central.connect.executor

import android.bluetooth.BluetoothAdapter
import android.content.Context
import java.util.concurrent.ConcurrentHashMap

object ConnectExecutorFactory {
    private val connectExecutors = ConcurrentHashMap<String, AbstractConnectExecutor>()

    /**
     * 每个 address 对应一个单例
     */
    fun get(context: Context, address: String?): AbstractConnectExecutor {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw UnsupportedOperationException("invalid address：$address")
        }
        return connectExecutors[address] ?: synchronized(this) {
            connectExecutors[address] ?: ConnectExecutor(context.applicationContext, address).apply {
                connectExecutors[address] = this
            }
        }
    }

    fun getAll(): List<AbstractConnectExecutor> {
        return connectExecutors.values.toList()
    }

    internal fun remove(address: String?) {
        connectExecutors.remove(address)
    }

}
