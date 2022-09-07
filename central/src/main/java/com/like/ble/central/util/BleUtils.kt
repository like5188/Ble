package com.like.ble.central.util

import android.bluetooth.le.ScanCallback

fun getScanFailedString(errorCode: Int) = when (errorCode) {
    ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Fails to start scan as BLE scan with the same settings is already started by the app."
    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Fails to start scan as app cannot be registered."
    ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Fails to start scan due an internal error"
    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Fails to start power optimized scan as this feature is not supported."
    5 -> "Fails to start scan as it is out of hardware resources."
    6 -> "Fails to start scan as application tries to scan too frequently."
    else -> "unknown scan error"
}