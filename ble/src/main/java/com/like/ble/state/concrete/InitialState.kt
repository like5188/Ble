package com.like.ble.state.concrete

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import com.like.ble.command.concrete.InitCommand
import com.like.ble.state.State
import com.like.ble.utils.PermissionUtils
import com.like.ble.utils.bindToLifecycleOwner
import com.like.ble.utils.callback.RxCallback
import com.like.ble.utils.getBluetoothManager
import com.like.ble.utils.isBluetoothEnable

/**
 * 蓝牙初始状态
 * 可以进行初始化操作
 */
class InitialState : State() {
    private val mPermissionUtils: PermissionUtils by lazy { PermissionUtils(mActivity) }
    private val mRxCallback: RxCallback by lazy { RxCallback(mActivity) }

    @SuppressLint("CheckResult")
    override fun init(command: InitCommand) {
        super.init(command)
        mPermissionUtils.checkPermissions(
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            onDenied = {
                command.failure(Throwable("the permissions was denied."))
            },
            onError = {
                command.failure(it)
            },
            onGranted = {
                if (mActivity.isBluetoothEnable()) {// 蓝牙已经初始化
                    command.success()
                    return@checkPermissions
                }

                val bluetoothManager = mActivity.getBluetoothManager()
                if (bluetoothManager == null) {
                    command.failure("failed to get BluetoothManager")
                    return@checkPermissions
                }

                if (bluetoothManager.adapter == null) {
                    command.failure("failed to get BluetoothAdapter")
                    return@checkPermissions
                }

                if (mActivity.isBluetoothEnable()) {// 蓝牙初始化成功
                    command.success()
                } else {// 蓝牙功能未打开
                    // 弹出开启蓝牙的对话框
                    mRxCallback.startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        .subscribe(
                            {
                                if (it.resultCode == Activity.RESULT_OK) {
                                    command.success()
                                } else {
                                    command.failure(Throwable("failed to open Bluetooth"))
                                }
                            },
                            {
                                command.failure(it)
                            }
                        ).bindToLifecycleOwner(mActivity)
                }
            })
    }

}