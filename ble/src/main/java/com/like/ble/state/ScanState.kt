package com.like.ble.state

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.like.ble.model.*
import com.like.ble.utils.getBluetoothAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙扫描状态
 * 可以进行扫描操作
 */
class ScanState(
    private val mActivity: FragmentActivity,
    private val mBleResultLiveData: MutableLiveData<BleResult>
) : BleStateAdapter() {
    private val mScanning = AtomicBoolean(false)
    private var mBleStartScanCommand: BleStartScanCommand? = null
    private val mScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            mBleStartScanCommand?.onSuccess?.invoke(BleScanResult(result.device, result.rssi, result.scanRecord?.bytes))
        }
    }
    private val mLeScanCallback: BluetoothAdapter.LeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        mBleStartScanCommand?.onSuccess?.invoke(BleScanResult(device, rssi, scanRecord))
    }

    override fun startScan(command: BleStartScanCommand) {
        if (mScanning.compareAndSet(false, true)) {
            mBleStartScanCommand = command
            mBleResultLiveData.postValue(BleResult(BleStatus.START_SCAN_DEVICE))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(mScanCallback)
            } else {
                mActivity.getBluetoothAdapter()?.startLeScan(mLeScanCallback)
            }
            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                // 在指定超时时间时取消扫描
                delay(command.scanTimeout)
                if (mScanning.get()) {
                    stopScan(BleStopScanCommand())
                }
            }
        }
    }

    override fun stopScan(command: BleStopScanCommand) {
        if (mScanning.compareAndSet(true, false)) {
            mBleResultLiveData.postValue(BleResult(BleStatus.STOP_SCAN_DEVICE))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(mScanCallback)
            } else {
                mActivity.getBluetoothAdapter()?.stopLeScan(mLeScanCallback)
            }
        }
    }

    override fun close() {
        stopScan(BleStopScanCommand())
    }

}