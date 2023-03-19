package com.like.ble.central.scan.result

import android.bluetooth.BluetoothDevice
import com.like.ble.central.util.scanrecordcompat.ScanRecordBelow21
import com.like.ble.exception.BleException

sealed class ScanResult {
    /**
     * 准备好开始扫描了
     */
    object Ready : ScanResult()

    /**
     * 扫描成功完成了
     */
    object Completed : ScanResult()

    /**
     * 扫描结果返回
     */
    data class Result(val device: BluetoothDevice, val rssi: Int, val scanRecord: ScanRecordBelow21?) : ScanResult()

    /**
     * 扫描出错了
     */
    class Error(val throwable: Throwable) : ScanResult() {
        constructor(message: String, code: Int = -1) : this(BleException(message, code))
    }

}
