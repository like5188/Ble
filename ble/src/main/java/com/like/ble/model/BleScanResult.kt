package com.like.ble.model

import android.bluetooth.BluetoothDevice

data class BleScanResult(val device: BluetoothDevice, val rssi: Int, val scanRecord: ByteArray?) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BleScanResult) return false

        if (device != other.device) return false
        if (rssi != other.rssi) return false
        if (scanRecord != null) {
            if (other.scanRecord == null) return false
            if (!scanRecord.contentEquals(other.scanRecord)) return false
        } else if (other.scanRecord != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + rssi
        result = 31 * result + (scanRecord?.contentHashCode() ?: 0)
        return result
    }

}