package com.like.ble.central.scan.executor

import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import com.like.ble.callback.BleCallback
import com.like.ble.central.scan.callback.ScanCallbackManager
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleException
import com.like.ble.util.getBluetoothAdapter
import kotlinx.coroutines.CancellableContinuation
import java.util.*
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙扫描的真正逻辑
 */
@SuppressLint("MissingPermission")
internal class ScanExecutor(context: Context) : BaseScanExecutor(context) {
    private val scanCallbackManager: ScanCallbackManager by lazy {
        ScanCallbackManager()
    }

    override fun onStartScan(continuation: CancellableContinuation<Unit>, filterServiceUuid: UUID?, onResult: (ScanResult) -> Unit) {
        val bleCallback = object : BleCallback<ScanResult>() {
            override fun onSuccess(data: ScanResult) {
                onResult(data)
            }

            override fun onError(exception: BleException) {
                // 这里不能直接抛异常，因为异步回调在另外的线程中，直接抛出了捕获不了，会造成崩溃。
                if (continuation.isActive)
                    continuation.resumeWithException(exception)
            }
        }
        scanCallbackManager.setScanBleCallback(bleCallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            onStartScan21(filterServiceUuid)
        } else {
            onStartScanBelow21(filterServiceUuid)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun onStartScan21(filterServiceUuid: UUID?) {
        val bluetoothLeScanner = mContext.getBluetoothAdapter()?.bluetoothLeScanner
            ?: throw BleException("phone does not support bluetooth scan")
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

    private fun onStartScanBelow21(filterServiceUuid: UUID?) {
        // startLeScan 方法实际上最终也是调用的 bluetoothLeScanner?.startScan 方法。只是忽略掉了错误回调，只处理了成功回调。所以不完善。
        val success = if (filterServiceUuid == null) {
            mContext.getBluetoothAdapter()?.startLeScan(scanCallbackManager.getLeScanCallback())
        } else {
            mContext.getBluetoothAdapter()?.startLeScan(arrayOf(filterServiceUuid), scanCallbackManager.getLeScanCallback())
        } ?: false
        if (!success) {
            throw BleException("开启扫描失败")
        }
    }

    override fun onStopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mContext.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(scanCallbackManager.getScanCallback())
        } else {
            mContext.getBluetoothAdapter()?.stopLeScan(scanCallbackManager.getLeScanCallback())
        }
    }

}
