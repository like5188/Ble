package com.like.ble.central.scan.callback

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import com.like.ble.callback.BleCallback
import com.like.ble.central.scan.result.ScanResult

class ScanCallbackManager {
    private val scanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
            scanBleCallback?.onSuccess(ScanResult(result.device, result.rssi, result.scanRecord?.bytes))
        }

        override fun onScanFailed(errorCode: Int) {
            val errorMsg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Fails to start scan as BLE scan with the same settings is already started by the app."
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Fails to start scan as app cannot be registered."
                SCAN_FAILED_INTERNAL_ERROR -> "Fails to start scan due an internal error"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Fails to start power optimized scan as this feature is not supported."
                5 -> "Fails to start scan as it is out of hardware resources."
                6 -> "Fails to start scan as application tries to scan too frequently."
                else -> "unknown scan error"
            }
            scanBleCallback?.onError(errorMsg, errorCode)
        }

        // Bluetoothadapter.isOffloadedScanBatchingSupported()</br>
        // 判断当前设备蓝牙芯片是否支持批处理扫描。如果支持则使用批处理扫描，可通过ScanSettings.Builder对象调用setReportDelay(Long)方法来设置蓝牙LE扫描的报告延迟的时间（以毫秒为单位）来启动批处理扫描模式。
        //
        // ScanSettings.Builder.setReportDelay(Long);
        // 当设备蓝牙芯片支持批处理扫描时，用来设置蓝牙LE扫描的报告延迟的时间（以毫秒为单位）。</br>
        // 该参数默认为 0，如果不修改它的值，则默认只会在onScanResult(int,ScanResult)中返回扫描到的蓝牙设备，不会触发onBatchScanResults(List)方法。
        // 设置为0以立即通知结果,不开启批处理扫描模式。即ScanCallback蓝牙回调中，不会触发onBatchScanResults(List)方法，但会触发onScanResult(int,ScanResult)方法，返回扫描到的蓝牙设备。
        // 当设置的时间大于0L时，则会开启批处理扫描模式。即触发onBatchScanResults(List)方法，返回扫描到的蓝牙设备列表。但不会触发onScanResult(int,ScanResult)方法。</br>
        override fun onBatchScanResults(results: MutableList<android.bluetooth.le.ScanResult>?) {
        }
    }
    private val leScanCallback: BluetoothAdapter.LeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        scanBleCallback?.onSuccess(ScanResult(device, rssi, scanRecord))
    }
    private val classicBroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0)
                    scanBleCallback?.onSuccess(ScanResult(device, rssi.toInt(), null))
                }
            }
        }
    }

    private var scanBleCallback: BleCallback<ScanResult>? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getScanCallback(): ScanCallback {
        return scanCallback
    }

    fun getLeScanCallback(): BluetoothAdapter.LeScanCallback {
        return leScanCallback
    }

    fun registerClassicBroadcastReceiver(context: Context) {
        context.registerReceiver(classicBroadcastReceiver, IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND) // 发现设备
        })
    }

    fun unRegisterClassicBroadcastReceiver(context: Context) {
        try {
            context.unregisterReceiver(classicBroadcastReceiver)
        } catch (e: Exception) {
        }
    }

    fun setScanBleCallback(callback: BleCallback<ScanResult>?) {
        scanBleCallback = callback
    }

}
