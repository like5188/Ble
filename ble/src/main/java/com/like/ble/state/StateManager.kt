package com.like.ble.state

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.like.ble.command.Command
import com.like.ble.command.CommandInvoker
import com.like.ble.command.concrete.*
import com.like.ble.state.concrete.AdvertisingState
import com.like.ble.state.concrete.ConnectState
import com.like.ble.state.concrete.ScanState
import com.like.ble.utils.PermissionUtils
import com.like.ble.utils.bindToLifecycleOwner
import com.like.ble.utils.callback.RxCallback
import com.like.ble.utils.isBluetoothEnable
import com.like.ble.utils.isSupportBluetooth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 蓝牙状态管理，并用[CommandInvoker]来执行对应的命令。
 */
class StateManager(private val mActivity: FragmentActivity) {
    private val mPermissionUtils: PermissionUtils by lazy { PermissionUtils(mActivity) }
    private val mRxCallback: RxCallback by lazy { RxCallback(mActivity) }
    private val mCommandInvoker: CommandInvoker by lazy { CommandInvoker(mActivity) }
    private var mCurState: State? = null
    private val mAdvertisingState: AdvertisingState by lazy {
        AdvertisingState().also {
            it.mActivity = mActivity
        }
    }
    private val mScanState: ScanState by lazy {
        ScanState().also {
            it.mActivity = mActivity
        }
    }
    private val mConnectStateMap = mutableMapOf<String, ConnectState>()

    suspend fun execute(command: Command) {
        if (!mActivity.isSupportBluetooth()) {
            command.failureAndComplete("手机不支持蓝牙")
            return
        }
        try {
            if (!checkPermissions()) {
                command.failureAndComplete("蓝牙权限被拒绝")
                return
            }
        } catch (e: Exception) {
            command.failureAndComplete("请求蓝牙权限失败")
            return
        }
        try {
            if (!isBleOpened()) {
                command.failureAndComplete("蓝牙未打开")
                return
            }
        } catch (e: Exception) {
            command.failureAndComplete("打开蓝牙失败")
            return
        }

        val state = getStateByCommand(command)
        if (state == null) {
            command.failureAndComplete("更新蓝牙状态失败")
            return
        }
        mCurState = state
        command.mReceiver = state
        mCommandInvoker.addCommand(command)
    }

    private fun getStateByCommand(command: Command): State? {
        return when (command) {
            is StartAdvertisingCommand, is StopAdvertisingCommand -> {
                mAdvertisingState
            }
            is StartScanCommand, is StopScanCommand -> {
                mScanState
            }
            is ConnectCommand -> {
                getConnectStateByAddress(command.address)
            }
            is DisconnectCommand -> {
                getConnectStateByAddress(command.address)
            }
            is ReadCharacteristicCommand -> {
                getConnectStateByAddress(command.address)
            }
            is WriteCharacteristicCommand -> {
                getConnectStateByAddress(command.address)
            }
            is SetMtuCommand -> {
                getConnectStateByAddress(command.address)
            }
            is CloseCommand -> {
                mCurState
            }
            else -> null
        }
    }

    private fun getConnectStateByAddress(address: String): ConnectState? {
        if (!mConnectStateMap.containsKey(address)) {
            mConnectStateMap[address] = ConnectState().also { it.mActivity = mActivity }
        }
        return mConnectStateMap[address]
    }

    fun close() {
        mCommandInvoker.close()
        mAdvertisingState.close(CloseCommand())
        mScanState.close(CloseCommand())
        mConnectStateMap.forEach {
            it.value.close(CloseCommand())
        }
        mConnectStateMap.clear()
        mCurState = null
    }

    private suspend fun checkPermissions() = suspendCoroutine<Boolean> { continuation ->
        mPermissionUtils.checkPermissions(
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
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