package com.like.ble.central.scan.executor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.like.ble.callback.BleCallback
import com.like.ble.central.scan.callback.ScanCallbackManager
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.central.util.hasGps
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
        if (mContext.hasGps()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.i("ScanExecutor", "onStartScan21")
                onStartScan21()
            } else {
                Log.i("ScanExecutor", "onStartScanBelow21")
                onStartScanBelow21()
            }
        } else {// 如果没有 gps 模块，则需要使用经典蓝牙扫描，需要注册广播接收器来接收扫描结果。比如工控机上面。
            Log.i("ScanExecutor", "onStartDiscovery")
            onStartDiscovery()
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

    private fun onStartDiscovery() {
        // startDiscovery在大多数手机上是可以同时发现经典蓝牙和Ble的，但是它是通过注册广播接收器来接收扫描结果的，
        // 无法返回Ble设备的广播数据，所以无法通过广播识别设备，且startDiscovery扫描Ble的效率比StartLeScan低很多。
        scanCallbackManager.registerClassicBroadcastReceiver(mContext)
        val success = mContext.getBluetoothAdapter()?.startDiscovery() ?: false
        if (!success) {
            scanCallbackManager.unRegisterClassicBroadcastReceiver(mContext)
            throw BleException("开启扫描失败")
        }
    }

    override fun onStopScan() {
        if (mContext.hasGps()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mContext.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(scanCallbackManager.getScanCallback())
            } else {
                mContext.getBluetoothAdapter()?.stopLeScan(scanCallbackManager.getLeScanCallback())
            }
        } else {// 如果没有 gps 模块，则需要使用经典蓝牙扫描。比如工控机上面。
            mContext.getBluetoothAdapter()?.cancelDiscovery()
            scanCallbackManager.unRegisterClassicBroadcastReceiver(mContext)
        }
    }

}
