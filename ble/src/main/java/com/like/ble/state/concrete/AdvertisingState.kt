package com.like.ble.state.concrete

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.concrete.CloseCommand
import com.like.ble.command.concrete.StartAdvertisingCommand
import com.like.ble.command.concrete.StopAdvertisingCommand
import com.like.ble.model.BleResult
import com.like.ble.model.BleStatus
import com.like.ble.state.StateAdapter
import com.like.ble.utils.getBluetoothAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙广播状态
 * 可以进行发送广播、停止广播操作
 */
class AdvertisingState : StateAdapter() {
    private val mIsRunning = AtomicBoolean(false)
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null

    private val mAdvertiseCallback: AdvertiseCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : AdvertiseCallback() {
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
            mLiveData.postValue(
                BleResult(
                    BleStatus.START_ADVERTISING_FAILURE,
                    errorMsg = errorMsg
                )
            )
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            mLiveData.postValue(BleResult(BleStatus.START_ADVERTISING_SUCCESS))
        }
    }

    override fun startAdvertising(command: StartAdvertisingCommand) {
        super.startAdvertising(command)
        if (mIsRunning.compareAndSet(false, true)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mLiveData.postValue(
                    BleResult(
                        BleStatus.START_ADVERTISING_FAILURE,
                        errorMsg = "phone does not support Bluetooth Advertiser"
                    )
                )
                return
            }
            if (mBluetoothLeAdvertiser == null) {
                mBluetoothLeAdvertiser = mActivity.getBluetoothAdapter()?.bluetoothLeAdvertiser
                if (mBluetoothLeAdvertiser == null) {
                    mLiveData.postValue(
                        BleResult(
                            BleStatus.START_ADVERTISING_FAILURE,
                            errorMsg = "phone does not support Bluetooth Advertiser"
                        )
                    )
                    return
                }
            }

            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                mBluetoothLeAdvertiser?.startAdvertising(
                    command.settings,
                    command.advertiseData,
                    command.scanResponse,
                    mAdvertiseCallback
                )
            }
        }
    }

    override fun stopAdvertising(command: StopAdvertisingCommand) {
        super.stopAdvertising(command)
        if (mIsRunning.compareAndSet(true, false)) {
            mLiveData.postValue(BleResult(BleStatus.STOP_ADVERTISING))

            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mBluetoothLeAdvertiser?.stopAdvertising(mAdvertiseCallback)
                }
            }
        }
    }

    override fun close(command: CloseCommand) {
        super.close(command)
        stopAdvertising(StopAdvertisingCommand())
        mBluetoothLeAdvertiser = null
    }

}