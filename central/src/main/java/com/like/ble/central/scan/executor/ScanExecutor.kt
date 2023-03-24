package com.like.ble.central.scan.executor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.like.ble.callback.BleCallback
import com.like.ble.central.scan.callback.ScanCallbackManager
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleException
import com.like.ble.util.getBluetoothAdapter
import kotlinx.coroutines.CancellableContinuation
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙扫描的真正逻辑
 */
@SuppressLint("MissingPermission")
class ScanExecutor(activity: ComponentActivity) : BaseScanExecutor(activity) {
    private val scanCallbackManager: ScanCallbackManager by lazy {
        ScanCallbackManager()
    }

    override fun onStartScan(filterServiceUuid: UUID?, onResult: (ScanResult) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            onStartScan21(filterServiceUuid, onResult)
        } else {
            onStartScanBelow21(filterServiceUuid, onResult)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun onStartScan21(
        filterServiceUuid: UUID?,
        onResult: (ScanResult) -> Unit
    ) {
        val bluetoothLeScanner = activity.getBluetoothAdapter()?.bluetoothLeScanner
            ?: throw BleException("phone does not support bluetooth scan")
        scanCallbackManager.setScanBleCallback(object : BleCallback<ScanResult>() {
            override fun onSuccess(data: ScanResult) {
                onResult(data)
            }

            override fun onError(exception: BleException) {
                throw exception
            }
        })
        if (filterServiceUuid == null) {
            bluetoothLeScanner.startScan(scanCallbackManager.getScanCallback())
        } else {
            // serviceUuid 只能在这里过滤，不能放到 filterScanResult() 方法中去，因为只有 gatt.discoverServices() 过后，device.getUuids() 方法才不会返回 null。
            bluetoothLeScanner.startScan(
                listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(filterServiceUuid)).build()),
                /*
                 setScanMode() 设置扫描模式。可选择的模式有三种：
                    ScanSettings.SCAN_MODE_LOW_POWER 低功耗模式
                    ScanSettings.SCAN_MODE_BALANCED 平衡模式
                    ScanSettings.SCAN_MODE_LOW_LATENCY 高功耗模式
                    从上到下，会越来越耗电,但扫描间隔越来越短，即扫描速度会越来越快。

                 setCallbackType() 设置回调类型，可选择的类型有三种：
                    ScanSettings.CALLBACK_TYPE_ALL_MATCHES 数值: 1 寻找符合过滤条件的蓝牙广播，如果没有设置过滤条件，则返回全部广播包
                    ScanSettings.CALLBACK_TYPE_FIRST_MATCH 数值: 2 仅针对与筛选条件匹配的第一个广播包触发结果回调。
                    ScanSettings.CALLBACK_TYPE_MATCH_LOST 数值: 4
                    回调类型
                    一般设置ScanSettings.CALLBACK_TYPE_ALL_MATCHES，有过滤条件时过滤，返回符合过滤条件的蓝牙广播。无过滤条件时，返回全部蓝牙广播。

                 setMatchMode() 设置蓝牙LE扫描滤波器硬件匹配的匹配模式，一般设置ScanSettings.MATCH_MODE_STICKY
                 */
                ScanSettings.Builder().build(),
                scanCallbackManager.getScanCallback()
            )
        }
    }

    private fun onStartScanBelow21(
        filterServiceUuid: UUID?,
        onResult: (ScanResult) -> Unit
    ) {
        scanCallbackManager.setLeScanBleCallback(object : BleCallback<ScanResult>() {
            override fun onSuccess(data: ScanResult) {
                onResult(data)
            }
        })
        // startLeScan 方法实际上最终也是调用的 bluetoothLeScanner?.startScan 方法。只是忽略掉了错误回调，只处理了成功回调。所以不完善。
        val success = if (filterServiceUuid == null) {
            activity.getBluetoothAdapter()?.startLeScan(scanCallbackManager.getLeScanCallback())
        } else {
            activity.getBluetoothAdapter()?.startLeScan(arrayOf(filterServiceUuid), scanCallbackManager.getLeScanCallback())
        } ?: false
        if (!success) {
            throw BleException("开启扫描失败")
        }
    }

    override fun onStartScan(continuation: CancellableContinuation<ScanResult>, address: String?) {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw BleException("invalid address：$address")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            onStartScan21(continuation, address)
        } else {
            onStartScanBelow21(continuation, address)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun onStartScan21(
        continuation: CancellableContinuation<ScanResult>,
        address: String,
    ) {
        val bluetoothLeScanner = activity.getBluetoothAdapter()?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            continuation.resumeWithException(BleException("phone does not support bluetooth scan"))
            return
        }
        scanCallbackManager.setScanBleCallback(object : BleCallback<ScanResult>() {
            override fun onSuccess(data: ScanResult) {
                if (address == data.device.address) {
                    onStopScan()
                    if (continuation.isActive)
                        continuation.resume(data)
                }
            }

            override fun onError(exception: BleException) {
                if (continuation.isActive)
                    continuation.resumeWithException(exception)
            }
        })
        bluetoothLeScanner.startScan(scanCallbackManager.getScanCallback())
    }

    private fun onStartScanBelow21(
        continuation: CancellableContinuation<ScanResult>,
        address: String,
    ) {
        scanCallbackManager.setLeScanBleCallback(object : BleCallback<ScanResult>() {
            override fun onSuccess(data: ScanResult) {
                if (address == data.device.address) {
                    onStopScan()
                    if (continuation.isActive)
                        continuation.resume(data)
                }
            }
        })
        // startLeScan 方法实际上最终也是调用的 bluetoothLeScanner?.startScan 方法。只是忽略掉了错误回调，只处理了成功回调。所以不完善。
        val success = activity.getBluetoothAdapter()?.startLeScan(scanCallbackManager.getLeScanCallback()) ?: false
        if (!success) {
            continuation.resumeWithException(BleException("开启扫描失败"))
        }
    }

    override fun onStopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(scanCallbackManager.getScanCallback())
        } else {
            activity.getBluetoothAdapter()?.stopLeScan(scanCallbackManager.getLeScanCallback())
        }
    }

}
