package com.like.ble.state

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.model.BleResult
import com.like.ble.model.BleStatus
import com.like.ble.utils.PermissionUtils
import com.like.ble.utils.bindToLifecycleOwner
import com.like.ble.utils.callback.RxCallback
import com.like.ble.utils.getBluetoothManager

/**
 * 蓝牙初始状态
 * 可以进行初始化操作
 */
class InitialState(
    private val mActivity: FragmentActivity,
    private val mBleResultLiveData: MutableLiveData<BleResult>
) : BaseBleState() {
    private val mPermissionUtils: PermissionUtils by lazy { PermissionUtils(mActivity) }
    private val mRxCallback: RxCallback by lazy { RxCallback(mActivity) }

    @SuppressLint("CheckResult")
    override fun init() {
        if (!isSupportBle()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "phone does not support Bluetooth"))
            return
        }

        mPermissionUtils.checkPermissions(
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            onDenied = {
                mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "the permissions was denied."))
            },
            onError = {
                mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = it.message ?: "unknown error"))
            },
            onGranted = {
                if (isBlueEnable()) {// 蓝牙已经初始化
                    mBleResultLiveData.postValue(BleResult(BleStatus.INIT_SUCCESS))
                    return@checkPermissions
                }

                val bluetoothManager = mActivity.getBluetoothManager()
                if (bluetoothManager == null) {
                    mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "failed to get BluetoothManager"))
                    return@checkPermissions
                }

                if (bluetoothManager.adapter == null) {
                    mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "failed to get BluetoothAdapter"))
                    return@checkPermissions
                }

                if (isBlueEnable()) {// 蓝牙初始化成功
                    mBleResultLiveData.postValue(BleResult(BleStatus.INIT_SUCCESS))
                } else {// 蓝牙功能未打开
                    // 弹出开启蓝牙的对话框
                    mRxCallback.startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)).subscribe(
                        {
                            if (it.resultCode == Activity.RESULT_OK) {
                                mBleResultLiveData.postValue(BleResult(BleStatus.INIT_SUCCESS))
                            } else {
                                mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "failed to open Bluetooth"))
                            }
                        },
                        {
                            mBleResultLiveData.postValue(
                                BleResult(
                                    BleStatus.INIT_FAILURE, errorMsg = it.message
                                        ?: "unknown error"
                                )
                            )
                        }
                    ).bindToLifecycleOwner(mActivity)
                }
            })
    }

    /**
     * 蓝牙是否就绪
     */
    private fun isBlueEnable(): Boolean = mActivity.getBluetoothManager()?.adapter?.isEnabled ?: false

    private fun isSupportBle() = mActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
}