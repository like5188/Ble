package com.like.ble.executor

import android.content.Context
import com.like.ble.util.BleBroadcastReceiverManager

/**
 * 蓝牙命令真正的执行者。
 */
abstract class BleExecutor(context: Context) {
    protected val mContext: Context = context.applicationContext
    private val bleBroadcastReceiverManager by lazy {
        BleBroadcastReceiverManager(mContext,
            onBleOn = {
            },
            onBleOff = {
                onBleOff()
            }
        )
    }

    init {
        bleBroadcastReceiverManager.register()
    }

    /**
     * 释放资源，并取消蓝牙开关广播监听
     */
    open fun close() {
        bleBroadcastReceiverManager.unregister()
    }

    protected open fun onBleOff() {

    }

}
