package com.like.ble.executor

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.callback.OnBleEnableListener
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
    private var onBleEnableListener: OnBleEnableListener? = null
    private val bleBroadcastReceiverManager by lazy {
        BleBroadcastReceiverManager(mContext,
            onBleOn = {
                onBleEnableListener?.on()
            },
            onBleOff = {
                onBleEnableListener?.off()
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

    /**
     * 检查蓝牙操作需要的环境，如果不满足，则去请求。
     * 1、如果没有打开蓝牙开关，则去打开。
     * 2、如果没有相关权限，则去请求。
     */
    fun requestEnvironment(activity: ComponentActivity?) {
        activity ?: return
        activity.lifecycleScope.launch(Dispatchers.Main) {
            activity.isBluetoothEnableAndSettingIfDisabled()
            PermissionUtils.requestPermissions(activity, *getPermissions())
        }
    }

    protected abstract fun getPermissions(): Array<String>

    protected fun checkEnvironment(): Boolean {
        return mContext.isBluetoothEnable() &&
                PermissionUtils.checkPermissions(mContext, *getPermissions())
    }

    protected fun checkEnvironmentOrThrow() {
        if (!mContext.isBluetoothEnable()) {
            throw BleExceptionDisabled
        }
        if (!PermissionUtils.checkPermissions(mContext, *getPermissions())) {
            throw BleExceptionPermission
        }
    }

    fun setOnBleEnableListener(listener: OnBleEnableListener?) {
        onBleEnableListener = listener
    }

    abstract fun onBleOff()

}
