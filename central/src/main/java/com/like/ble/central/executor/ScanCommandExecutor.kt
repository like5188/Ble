package com.like.ble.central.executor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.like.ble.central.command.StartScanCommand
import com.like.ble.central.command.StopScanCommand
import com.like.ble.util.BleBroadcastReceiverManager
import com.like.ble.util.getBluetoothAdapter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙扫描相关的命令执行者
 * 可以进行扫描、停止扫描操作
 */
@SuppressLint("MissingPermission")
class ScanCommandExecutor(private val mActivity: ComponentActivity) : CentralCommandExecutor() {
    private val mScanning = AtomicBoolean(false)
    private val mBleBroadcastReceiverManager: BleBroadcastReceiverManager by lazy {
        BleBroadcastReceiverManager(mActivity,
            onBleOff = {
                mScanning.set(false)
                startScanCommand?.error("蓝牙功能已关闭")
                stopScanCommand?.error("蓝牙功能已关闭")
            }
        )
    }
    private val mScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            filterScanResult(result.device, result.rssi, result.scanRecord?.bytes)
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
            startScanCommand?.error(errorMsg)
            mScanning.set(false)
        }

        // Bluetoothadapter.isOffloadedScanBatchingSupported()</br>
        // 判断当前设备蓝牙芯片是否支持批处理扫描。如果支持则使用批处理扫描，可通过ScanSettings.Builder对象调用setReportDelay(Long)方法来设置蓝牙LE扫描的报告延迟的时间（以毫秒为单位）来启动批处理扫描模式。
        //
        // ScanSettings.Builder.setReportDelay(Long);
        // 当设备蓝牙芯片支持批处理扫描时，用来设置蓝牙LE扫描的报告延迟的时间（以毫秒为单位）。</br>
        // 该参数默认为 0，如果不修改它的值，则默认只会在onScanResult(int,ScanResult)中返回扫描到的蓝牙设备，不会触发onBatchScanResults(List)方法。
        // 设置为0以立即通知结果,不开启批处理扫描模式。即ScanCallback蓝牙回调中，不会触发onBatchScanResults(List)方法，但会触发onScanResult(int,ScanResult)方法，返回扫描到的蓝牙设备。
        // 当设置的时间大于0L时，则会开启批处理扫描模式。即触发onBatchScanResults(List)方法，返回扫描到的蓝牙设备列表。但不会触发onScanResult(int,ScanResult)方法。</br>
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
        }
    }
    private val mLeScanCallback: BluetoothAdapter.LeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        filterScanResult(device, rssi, scanRecord)
    }

    init {
        mBleBroadcastReceiverManager.register()
    }

    @Synchronized
    private fun filterScanResult(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {
        val startScanCommand = startScanCommand ?: return
        // 设备名字匹配
        if (startScanCommand.filterDeviceName.isNotEmpty()) {
            val deviceName = device.name ?: ""
            if (startScanCommand.fuzzyMatchingDeviceName) {// 模糊匹配
                if (!deviceName.contains(startScanCommand.filterDeviceName)) {
                    return
                }
            } else {
                if (deviceName != startScanCommand.filterDeviceName) {
                    return
                }
            }
        }
        // 设备地址匹配
        if (startScanCommand.filterDeviceAddress.isNotEmpty() && device.address != startScanCommand.filterDeviceAddress) {
            return
        }
        startScanCommand.result(device, rssi, scanRecord)
    }

    @Synchronized
    override fun startScan(command: StartScanCommand) {
        if (mScanning.compareAndSet(false, true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (command.filterServiceUuid == null) {
                    mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(mScanCallback)
                } else {
                    // serviceUuid 只能在这里过滤，不能放到 filterScanResult() 方法中去，因为只有 gatt.discoverServices() 过后，device.getUuids() 方法才不会返回 null。
                    mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(
                        listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(command.filterServiceUuid)).build()),
                        // setScanMode() 设置扫描模式。可选择的模式有三种：
                        // ScanSettings.SCAN_MODE_LOW_POWER 低功耗模式
                        // ScanSettings.SCAN_MODE_BALANCED 平衡模式
                        // ScanSettings.SCAN_MODE_LOW_LATENCY 高功耗模式
                        // 从上到下，会越来越耗电,但扫描间隔越来越短，即扫描速度会越来越快。
                        // setCallbackType() 设置回调类型，可选择的类型有三种：
                        // ScanSettings.CALLBACK_TYPE_ALL_MATCHES 数值: 1 寻找符合过滤条件的蓝牙广播，如果没有设置过滤条件，则返回全部广播包
                        // ScanSettings.CALLBACK_TYPE_FIRST_MATCH 数值: 2 仅针对与筛选条件匹配的第一个广播包触发结果回调。
                        // ScanSettings.CALLBACK_TYPE_MATCH_LOST 数值: 4
                        // 回调类型一般设置ScanSettings.CALLBACK_TYPE_ALL_MATCHES，有过滤条件时过滤，返回符合过滤条件的蓝牙广播。无过滤条件时，返回全部蓝牙广播。
                        // setMatchMode() 设置蓝牙LE扫描滤波器硬件匹配的匹配模式，一般设置ScanSettings.MATCH_MODE_STICKY
                        ScanSettings.Builder().build(),
                        mScanCallback
                    )
                }
            } else {
                if (command.filterServiceUuid == null) {
                    if (mActivity.getBluetoothAdapter()?.startLeScan(mLeScanCallback) != true) {
                        command.error("扫描失败")
                        return
                    }
                } else {
                    if (mActivity.getBluetoothAdapter()?.startLeScan(arrayOf(command.filterServiceUuid), mLeScanCallback) != true) {
                        command.error("扫描失败")
                        return
                    }
                }
            }
        }
        command.complete()// 这里直接完成命令，避免扫描不到需要的设备时，无法触发 StartScanCommand.onResult(device, rssi, scanRecord)
    }

    @Synchronized
    override fun stopScan(command: StopScanCommand) {
        if (mScanning.compareAndSet(true, false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(mScanCallback)
            } else {
                mActivity.getBluetoothAdapter()?.stopLeScan(mLeScanCallback)
            }
        }
        command.complete()
    }

    @Synchronized
    override fun close() {
        stopScan(StopScanCommand())
        mBleBroadcastReceiverManager.unregister()
        super.close()
    }

}