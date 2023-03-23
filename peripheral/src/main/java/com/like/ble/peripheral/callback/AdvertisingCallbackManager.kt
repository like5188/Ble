package com.like.ble.peripheral.callback

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import com.like.ble.callback.BleCallback

class AdvertisingCallbackManager {
    private val mAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            val errorMsg = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes."
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Failed to start advertising because no advertising instance is available."
                ADVERTISE_FAILED_ALREADY_STARTED -> "Failed to start advertising as the advertising is already started"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Operation failed due to an internal error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "This feature is not supported on this platform"
                else -> "errorCode=$errorCode"
            }
            advertisingCallback?.onError(errorMsg, errorCode)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            advertisingCallback?.onSuccess(Unit)
        }
    }
    private var advertisingCallback: BleCallback<Unit>? = null

    fun getAdvertiseCallback(): AdvertiseCallback {
        return mAdvertiseCallback
    }

    fun setAdvertisingCallback(callback: BleCallback<Unit>?) {
        advertisingCallback = callback
    }

}
