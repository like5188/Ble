package com.like.ble.central.scan.executor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.like.ble.callback.BleCallback
import com.like.ble.central.scan.callback.ScanCallbackManager
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleException
import com.like.ble.util.getBluetoothAdapter

/**
 * 蓝牙扫描的真正逻辑
 */
@SuppressLint("MissingPermission")
internal class ScanExecutor(context: Context) : BaseScanExecutor(context) {
    private val scanCallbackManager: ScanCallbackManager by lazy {
        ScanCallbackManager()
    }

    override fun onStartScan(
        onSuccess: ((ScanResult) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        val bleCallback = object : BleCallback<ScanResult>() {
            override fun onSuccess(data: ScanResult) {
                onSuccess?.invoke(data)
            }

            override fun onError(exception: BleException) {
                // 这里不能直接抛异常，因为异步回调在另外的线程中，直接抛出了捕获不了，会造成崩溃。
                onError?.invoke(exception)
            }
        }
        scanCallbackManager.setScanBleCallback(bleCallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            onStartScan21()
        } else {
            onStartScanBelow21()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun onStartScan21() {
        val bluetoothLeScanner = mContext.getBluetoothAdapter()?.bluetoothLeScanner
            ?: throw BleException("phone does not support bluetooth scan")
        bluetoothLeScanner.startScan(scanCallbackManager.getScanCallback())
    }

    private fun onStartScanBelow21() {
        // startLeScan 方法实际上最终也是调用的 bluetoothLeScanner?.startScan 方法。只是忽略掉了错误回调，只处理了成功回调。所以不完善。
        val success = mContext.getBluetoothAdapter()?.startLeScan(scanCallbackManager.getLeScanCallback()) ?: false
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
