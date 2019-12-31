package com.like.ble.invoker

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.base.Command
import com.like.ble.utils.PermissionUtils
import com.like.ble.utils.bindToLifecycleOwner
import com.like.ble.utils.callback.RxCallback
import com.like.ble.utils.isBluetoothEnable
import com.like.ble.utils.isSupportBluetooth
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 命令请求者
 */
abstract class Invoker(protected val mActivity: FragmentActivity) {
    private val mPermissionUtils: PermissionUtils by lazy { PermissionUtils(mActivity) }
    private val mRxCallback: RxCallback by lazy { RxCallback(mActivity) }

    fun addCommand(command: Command) {
        mActivity.lifecycleScope.launch {
            if (!mActivity.isSupportBluetooth()) {
                command.errorAndComplete("手机不支持蓝牙")
                return@launch
            }
            try {
                if (!checkPermissions()) {
                    command.errorAndComplete("蓝牙权限被拒绝")
                    return@launch
                }
            } catch (e: Exception) {
                command.errorAndComplete("请求蓝牙权限失败")
                return@launch
            }
            try {
                if (!isBleOpened()) {
                    command.errorAndComplete("蓝牙未打开")
                    return@launch
                }
            } catch (e: Exception) {
                command.errorAndComplete("打开蓝牙失败")
                return@launch
            }
            execute(command)
        }
    }

    protected abstract fun execute(command: Command)

    abstract fun close()

    private suspend fun checkPermissions() = suspendCoroutine<Boolean> { continuation ->
        mPermissionUtils.checkPermissions(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION,// android 6.0 以上需要获取到定位权限
            Manifest.permission.ACCESS_COARSE_LOCATION,
            onDenied = {
                continuation.resume(false)
            },
            onError = {
                continuation.resumeWithException(it)
            },
            onGranted = {
                continuation.resume(true)
            })
    }

    private suspend fun isBleOpened() = suspendCoroutine<Boolean> { continuation ->
        if (mActivity.isBluetoothEnable()) {
            continuation.resume(true)
        } else {// 蓝牙功能未打开
            // 弹出开启蓝牙的对话框
            mRxCallback.startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                .subscribe(
                    {
                        continuation.resume(it.resultCode == Activity.RESULT_OK)
                    },
                    {
                        continuation.resumeWithException(it)
                    }
                ).bindToLifecycleOwner(mActivity)
        }
    }

}