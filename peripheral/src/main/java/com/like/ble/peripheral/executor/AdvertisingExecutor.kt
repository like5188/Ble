package com.like.ble.peripheral.executor

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import androidx.activity.ComponentActivity
import com.like.ble.callback.BleCallback
import com.like.ble.exception.BleException
import com.like.ble.peripheral.callback.AdvertisingCallbackManager
import com.like.ble.util.getBluetoothAdapter
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 外围设备广播的真正逻辑
 */
@SuppressLint("MissingPermission")
class AdvertisingExecutor(activity: ComponentActivity) : BaseAdvertisingExecutor(activity) {
    private val advertisingCallbackManager: AdvertisingCallbackManager by lazy {
        AdvertisingCallbackManager()
    }

    override fun onStartAdvertising(
        continuation: CancellableContinuation<Unit>,
        settings: AdvertiseSettings,
        advertiseData: AdvertiseData,
        scanResponse: AdvertiseData?,
        deviceName: String
    ) {
        val bluetoothLeAdvertiser = activity.getBluetoothAdapter()?.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            continuation.resumeWithException(BleException("phone does not support bluetooth Advertiser"))
            return
        }
        // 设置设备名字
        if (deviceName.isNotEmpty()) {
            if (activity.getBluetoothAdapter()?.setName(deviceName) != true) {
                continuation.resumeWithException(BleException("set device name ($deviceName) error"))
                return
            }
        }
        advertisingCallbackManager.setAdvertisingCallback(object : BleCallback() {
            override fun onSuccess() {
                continuation.resume(Unit)
            }

            override fun onError(exception: BleException) {
                continuation.resumeWithException(exception)
            }
        })
        bluetoothLeAdvertiser.startAdvertising(
            settings,
            advertiseData,
            scanResponse,
            advertisingCallbackManager.getAdvertiseCallback()
        )
    }

    override fun onStopAdvertising() {
        activity.getBluetoothAdapter()?.bluetoothLeAdvertiser?.stopAdvertising(advertisingCallbackManager.getAdvertiseCallback())
    }

}
