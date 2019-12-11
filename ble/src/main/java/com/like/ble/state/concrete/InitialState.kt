package com.like.ble.state.concrete

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.command.concrete.InitCommand
import com.like.ble.model.BleResult
import com.like.ble.model.BleStatus
import com.like.ble.state.StateAdapter
import com.like.ble.utils.PermissionUtils
import com.like.ble.utils.bindToLifecycleOwner
import com.like.ble.utils.callback.RxCallback
import com.like.ble.utils.getBluetoothManager
import com.like.ble.utils.isBluetoothEnable

/**
 * 蓝牙初始状态
 * 可以进行初始化操作
 */
class InitialState(
    private val mActivity: FragmentActivity,
    private val mLiveData: MutableLiveData<BleResult>
) : StateAdapter() {
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
                mLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "the permissions was denied."))
            },
            onError = {
                mLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = it.message ?: "unknown error"))
            },
            onGranted = {
                if (mActivity.isBluetoothEnable()) {// 蓝牙已经初始化
                    mLiveData.postValue(BleResult(BleStatus.INIT_SUCCESS))
                    return@checkPermissions
                }

                val bluetoothManager = mActivity.getBluetoothManager()
                if (bluetoothManager == null) {
                    mLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "failed to get BluetoothManager"))
                    return@checkPermissions
                }

                if (bluetoothManager.adapter == null) {
                    mLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "failed to get BluetoothAdapter"))
                    return@checkPermissions
                }

                if (mActivity.isBluetoothEnable()) {// 蓝牙初始化成功
                    mLiveData.postValue(BleResult(BleStatus.INIT_SUCCESS))
                } else {// 蓝牙功能未打开
                    // 弹出开启蓝牙的对话框
                    mRxCallback.startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)).subscribe(
                        {
                            if (it.resultCode == Activity.RESULT_OK) {
                                mLiveData.postValue(BleResult(BleStatus.INIT_SUCCESS))
                            } else {
                                mLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "failed to open Bluetooth"))
                            }
                        },
                        {
                            mLiveData.postValue(
                                BleResult(BleStatus.INIT_FAILURE, errorMsg = it.message ?: "unknown error")
                            )
                        }
                    ).bindToLifecycleOwner(mActivity)
                }
            })
    }

}