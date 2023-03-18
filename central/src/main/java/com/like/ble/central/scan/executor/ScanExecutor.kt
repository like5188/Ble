package com.like.ble.central.scan.executor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import com.like.ble.central.scan.callback.ScanCallback
import com.like.ble.central.scan.callback.ScanCallbackManager
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionTimeout
import com.like.ble.result.BleResult
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import com.like.ble.util.getBluetoothAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙扫描相关的命令执行者
 * 可以进行扫描、停止扫描操作
 */
@SuppressLint("MissingPermission")
class ScanExecutor(activity: ComponentActivity) : AbstractScanExecutor(activity) {
    private val scanCallbackManager: ScanCallbackManager by lazy {
        ScanCallbackManager()
    }
    private val mutex = Mutex()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }
    private val _scanFlow: MutableSharedFlow<BleResult> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    override val scanFlow: Flow<BleResult> = _scanFlow

    override suspend fun startScan(filterServiceUuid: UUID?, duration: Long) {
        if (!mutex.tryLock()) {
            _scanFlow.tryEmit(BleResult.Error(BleExceptionBusy("正在扫描中……，请耐心等待！")))
            return
        }
        try {
            withContext(Dispatchers.IO) {
                checkEnvironmentOrThrow()
                suspendCancellableCoroutineWithTimeout.execute<Unit>(duration) { continuation ->
                    scanCallbackManager.setScanCallback(object : ScanCallback() {
                        override fun onSuccess(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {
                            _scanFlow.tryEmit(BleResult.Result(ScanResult(device, rssi, scanRecord)))
                        }

                        override fun onError(exception: BleException) {
                            continuation.resumeWithException(exception)
                        }
                    })
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (filterServiceUuid == null) {
                            activity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(scanCallbackManager.getScanCallback())
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
                                scanCallbackManager.getScanCallback()
                            )
                        }
                    } else {
                        if (filterServiceUuid == null) {
                            if (activity.getBluetoothAdapter()?.startLeScan(scanCallbackManager.getLeScanCallback()) != true) {
                                continuation.resumeWithException(BleException("扫描失败"))
                            }
                        } else {
                            if (activity.getBluetoothAdapter()
                                    ?.startLeScan(arrayOf(filterServiceUuid), scanCallbackManager.getLeScanCallback()) != true
                            ) {
                                continuation.resumeWithException(BleException("扫描失败"))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is BleExceptionTimeout) {
                stopScan()
            }
            _scanFlow.tryEmit(BleResult.Error(e))
        } finally {
            Log.e("TAG", "unlock")
            mutex.unlock()
        }
    }

    override fun stopScan() {
        if (!checkEnvironment()) {
            return
        }
        suspendCancellableCoroutineWithTimeout.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(scanCallbackManager.getScanCallback())
        } else {
            activity.getBluetoothAdapter()?.stopLeScan(scanCallbackManager.getLeScanCallback())
        }
    }

    override fun close() {
        stopScan()
    }

}
