package com.like.ble.peripheral.executor

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import com.like.ble.callback.BleCallback
import com.like.ble.exception.BleException
import com.like.ble.peripheral.callback.AdvertisingCallbackManager
import com.like.ble.util.getBluetoothAdapter

/**
 * 外围设备广播的真正逻辑
 */
@SuppressLint("MissingPermission")
internal class AdvertisingExecutor(context: Context) : BaseAdvertisingExecutor(context) {
    private val advertisingCallbackManager: AdvertisingCallbackManager by lazy {
        AdvertisingCallbackManager()
    }

    override fun onStartAdvertising(
        settings: AdvertiseSettings,
        advertiseData: AdvertiseData,
        scanResponse: AdvertiseData?,
        deviceName: String,
        onSuccess: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        val bluetoothLeAdvertiser = mContext.getBluetoothAdapter()?.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            onError?.invoke(BleException("phone does not support bluetooth Advertiser"))
            return
        }
        // 设置设备名字
        if (deviceName.isNotEmpty()) {
            if (mContext.getBluetoothAdapter()?.setName(deviceName) != true) {
                onError?.invoke(BleException("set device name ($deviceName) error"))
                return
            }
        }
        advertisingCallbackManager.setAdvertisingBleCallback(object : BleCallback<Unit>() {
            override fun onSuccess(data: Unit) {
                onSuccess?.invoke()
            }

            override fun onError(exception: BleException) {
                onError?.invoke(exception)
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
        mContext.getBluetoothAdapter()?.bluetoothLeAdvertiser?.stopAdvertising(advertisingCallbackManager.getAdvertiseCallback())
    }

}
