package com.like.ble.central.blestate

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.central.model.BleResult
import com.like.ble.central.model.BleStatus
import com.like.ble.central.utils.PermissionUtils
import com.like.ble.central.utils.bindToLifecycleOwner
import com.like.ble.central.utils.callback.RxCallback

/**
 * 蓝牙初始状态
 * 可以进行初始化操作
 */
class InitialState(
    private val mActivity: FragmentActivity,
    private val mBleResultLiveData: MutableLiveData<BleResult>
) : BaseBleState() {
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null

    private val mPermissionUtils: PermissionUtils by lazy { PermissionUtils(mActivity) }
    private val mRxCallback: RxCallback by lazy { RxCallback(mActivity) }

    @SuppressLint("CheckResult")
    override fun init() {
        mBleResultLiveData.postValue(BleResult(BleStatus.INIT))

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

                mBluetoothManager = mActivity.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                if (mBluetoothManager == null) {
                    mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "failed to get BluetoothManager"))
                    return@checkPermissions
                }

                mBluetoothAdapter = mBluetoothManager?.adapter
                if (mBluetoothAdapter == null) {
                    mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "failed to get BluetoothAdapter"))
                    return@checkPermissions
                }

                if (isBlueEnable()) {// 蓝牙初始化成功
                    mBleResultLiveData.postValue(BleResult(BleStatus.INIT_SUCCESS))
                } else {// 蓝牙功能未打开
                    mBluetoothManager = null
                    mBluetoothAdapter = null
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

    override fun close() {
        mBluetoothAdapter = null
        mBluetoothManager = null
    }

    override fun getBluetoothAdapter(): BluetoothAdapter? {
        return mBluetoothAdapter
    }

    override fun getBluetoothManager(): BluetoothManager? {
        return mBluetoothManager
    }

    /**
     * 蓝牙是否就绪
     */
    private fun isBlueEnable() = mBluetoothAdapter?.isEnabled ?: false

    private fun isSupportBle() = mActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
}