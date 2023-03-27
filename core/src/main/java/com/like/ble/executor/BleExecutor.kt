package com.like.ble.executor

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.exception.BleExceptionDisabled
import com.like.ble.exception.BleExceptionPermission
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

    /**
     * 释放资源
     */
    abstract fun close()

    /**
     * 检查蓝牙操作需要的环境
     * 1、如果没有打开蓝牙开关，则去打开。
     * 2、如果没有相关权限，则去请求。
     */
    fun checkEnvironment(activity: ComponentActivity?) {
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

}
