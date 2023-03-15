package com.like.ble.central.executor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.like.ble.central.result.ScanResult
import com.like.ble.central.util.PermissionUtils
import com.like.ble.result.BleResult
import com.like.ble.util.BleBroadcastReceiverManager
import com.like.ble.util.getBluetoothAdapter
import com.like.ble.util.isBluetoothEnableAndSettingIfDisabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙扫描相关的命令执行者
 * 可以进行扫描、停止扫描操作
 */
@SuppressLint("MissingPermission")
class ScanExecutor(private val activity: ComponentActivity, private val lifecycleScope: CoroutineScope) : ICentralExecutor {
    private val _scanFlow: MutableSharedFlow<BleResult> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    private val mScanning = AtomicBoolean(false)
    private val mBleBroadcastReceiverManager: BleBroadcastReceiverManager by lazy {
        BleBroadcastReceiverManager(activity.applicationContext,
            onBleOff = {
                mScanning.set(false)
                emit("蓝牙功能已关闭")
            }
        )
    }
    private val mScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
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
            mScanning.set(false)
            emit(errorMsg, errorCode)
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
    private val mLeScanCallback: BluetoothAdapter.LeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        filterScanResult(device, rssi, scanRecord)
    }
    private var filterDeviceName: String = ""
    private var fuzzyMatchingDeviceName: Boolean = false
    private var filterDeviceAddress: String = ""
    private var filterServiceUuid: UUID? = null


    override val scanFlow: Flow<BleResult> = _scanFlow
    override suspend fun startScan(
        filterDeviceName: String,
        fuzzyMatchingDeviceName: Boolean,
        filterDeviceAddress: String,
        filterServiceUuid: UUID?,
        duration: Long
    ) {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            emit("蓝牙未打开")
            return
        }
        if (!PermissionUtils.checkPermissions(activity, true)) {
            emit("蓝牙权限被拒绝")
            return
        }
        if (mScanning.compareAndSet(false, true)) {
            this.filterDeviceName = filterDeviceName
            this.fuzzyMatchingDeviceName = fuzzyMatchingDeviceName
            this.filterDeviceAddress = filterDeviceAddress
            this.filterServiceUuid = filterServiceUuid
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (filterServiceUuid == null) {
                    activity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(mScanCallback)
                } else {
                    // serviceUuid 只能在这里过滤，不能放到 filterScanResult() 方法中去，因为只有 gatt.discoverServices() 过后，device.getUuids() 方法才不会返回 null。
                    activity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(
                        listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(filterServiceUuid)).build()),
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
                if (filterServiceUuid == null) {
                    if (activity.getBluetoothAdapter()?.startLeScan(mLeScanCallback) != true) {
                        emit("扫描失败")
                    }
                } else {
                    if (activity.getBluetoothAdapter()?.startLeScan(arrayOf(filterServiceUuid), mLeScanCallback) != true) {
                        emit("扫描失败")
                    }
                }
            }
            delay(duration)
            stopScan()
        }
    }

    override suspend fun stopScan() {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            return
        }
        if (!PermissionUtils.checkPermissions(activity, true)) {
            return
        }
        if (mScanning.compareAndSet(true, false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(mScanCallback)
            } else {
                activity.getBluetoothAdapter()?.stopLeScan(mLeScanCallback)
            }
        }
    }

    init {
        mBleBroadcastReceiverManager.register()
    }

    @Synchronized
    private fun filterScanResult(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {
        // 设备名字匹配
        if (filterDeviceName.isNotEmpty()) {
            val deviceName = device.name ?: ""
            if (fuzzyMatchingDeviceName) {// 模糊匹配
                if (!deviceName.contains(filterDeviceName)) {
                    return
                }
            } else {
                if (deviceName != filterDeviceName) {
                    return
                }
            }
        }
        // 设备地址匹配
        if (filterDeviceAddress.isNotEmpty() && device.address != filterDeviceAddress) {
            return
        }
        emit(device, rssi, scanRecord)
    }

    private fun emit(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {
        lifecycleScope.launch(Dispatchers.Main) {
            _scanFlow.emit(BleResult.Success(ScanResult(device, rssi, scanRecord)))
        }
    }

    private fun emit(msg: String, code: Int = -1) {
        lifecycleScope.launch(Dispatchers.Main) {
            _scanFlow.emit(BleResult.Error(msg, code))
        }
    }

    override suspend fun close() {
        stopScan()
        mBleBroadcastReceiverManager.unregister()
    }

}
