package com.like.ble.executor

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.exception.BleExceptionDisabled
import com.like.ble.exception.BleExceptionPermission
import com.like.ble.util.BleBroadcastReceiverManager
import com.like.ble.util.PermissionUtils
import com.like.ble.util.isBluetoothEnable
import com.like.ble.util.isBluetoothEnableAndSettingIfDisabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    /**
     * 检查蓝牙操作需要的环境，如果不满足，则去请求。
     * 1、如果没有打开蓝牙开关，则去打开。
     * 2、如果没有相关权限，则去请求。
     */
    fun requestEnvironment(activity: ComponentActivity?) {
        activity ?: return
        activity.lifecycleScope.launch(Dispatchers.Main) {
            if (PermissionUtils.requestPermissions(activity, *getPermissions())) {
                activity.isBluetoothEnableAndSettingIfDisabled()
            }
        }
    }

    protected abstract fun getPermissions(): Array<String>

    protected fun checkEnvironment(): Boolean {
        return PermissionUtils.checkPermissions(mContext, *getPermissions()) && mContext.isBluetoothEnable()
    }

    protected fun checkEnvironmentOrThrow() {
        if (!PermissionUtils.checkPermissions(mContext, *getPermissions())) {
            throw BleExceptionPermission
        }
        if (!mContext.isBluetoothEnable()) {
            throw BleExceptionDisabled
        }
    }

}
