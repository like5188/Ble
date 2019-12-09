package com.like.ble.state

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.like.ble.model.BleResult
import com.like.ble.model.BleStatus
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙广播状态
 * 可以进行发送广播、停止广播操作
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AdvertisingState(
    private val mBleResultLiveData: MutableLiveData<BleResult>,
    private var mBluetoothManager: BluetoothManager?,
    private var mBluetoothAdapter: BluetoothAdapter?
) : BaseBleState() {
    private val mIsRunning = AtomicBoolean(false)
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null

    private val mAdvertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            val errorMsg = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes."
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Failed to start advertising because no advertising instance is available."
                ADVERTISE_FAILED_ALREADY_STARTED -> "Failed to start advertising as the advertising is already started"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Operation failed due to an internal error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "This feature is not supported on this platform"
                else -> "errorCode=$errorCode"
            }
            mBleResultLiveData.postValue(
                BleResult(
                    BleStatus.START_ADVERTISING_FAILURE,
                    errorMsg = errorMsg
                )
            )
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            mBleResultLiveData.postValue(BleResult(BleStatus.START_ADVERTISING_SUCCESS))
        }
    }

    override fun startAdvertising(settings: AdvertiseSettings, advertiseData: AdvertiseData, scanResponse: AdvertiseData) {
        if (mIsRunning.compareAndSet(false, true)) {
            if (mBluetoothLeAdvertiser == null) {
                mBluetoothLeAdvertiser = mBluetoothAdapter?.bluetoothLeAdvertiser
                if (mBluetoothLeAdvertiser == null) {
                    mBleResultLiveData.postValue(
                        BleResult(
                            BleStatus.START_ADVERTISING_FAILURE,
                            errorMsg = "phone does not support Bluetooth Advertiser"
                        )
                    )
                    return
                }
            }

            mBluetoothLeAdvertiser?.startAdvertising(settings, advertiseData, scanResponse, mAdvertiseCallback)
        }
    }

    override fun stopAdvertising() {
        if (mIsRunning.compareAndSet(true, false)) {
            mBleResultLiveData.postValue(BleResult(BleStatus.STOP_ADVERTISING))
            mBluetoothLeAdvertiser?.stopAdvertising(mAdvertiseCallback)
        }
    }

    override fun close() {
        stopAdvertising()
        mBluetoothLeAdvertiser = null
        mBluetoothAdapter = null
        mBluetoothManager = null
    }

    override fun getBluetoothAdapter(): BluetoothAdapter? {
        return mBluetoothAdapter
    }

    override fun getBluetoothManager(): BluetoothManager? {
        return mBluetoothManager
    }

}