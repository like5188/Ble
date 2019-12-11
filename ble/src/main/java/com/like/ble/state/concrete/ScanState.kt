package com.like.ble.state.concrete

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.concrete.CloseCommand
import com.like.ble.command.concrete.StartScanCommand
import com.like.ble.command.concrete.StopScanCommand
import com.like.ble.model.BleResult
import com.like.ble.model.BleStatus
import com.like.ble.state.StateAdapter
import com.like.ble.utils.getBluetoothAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙扫描状态
 * 可以进行扫描操作
 */
class ScanState : StateAdapter() {
    private val mScanning = AtomicBoolean(false)
    private var mStartScanCommand: StartScanCommand? = null
    private val mScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            mStartScanCommand?.onSuccess?.invoke(result.device, result.rssi, result.scanRecord?.bytes)
        }
    }
    private val mLeScanCallback: BluetoothAdapter.LeScanCallback =
        BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            mStartScanCommand?.onSuccess?.invoke(device, rssi, scanRecord)
        }

    override fun startScan(command: StartScanCommand) {
        super.startScan(command)
        if (mScanning.compareAndSet(false, true)) {
            mStartScanCommand = command
            mLiveData.postValue(BleResult(BleStatus.START_SCAN_DEVICE))

            mActivity.lifecycleScope.launch(Dispatchers.IO) {

                launch(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(mScanCallback)
                    } else {
                        mActivity.getBluetoothAdapter()?.startLeScan(mLeScanCallback)
                    }
                }

                launch(Dispatchers.IO) {
                    // 在指定超时时间时取消扫描
                    delay(command.scanTimeout)
                    if (mScanning.get()) {
                        stopScan(StopScanCommand())
                    }
                }
            }
        }
    }

    override fun stopScan(command: StopScanCommand) {
        super.stopScan(command)
        if (mScanning.compareAndSet(true, false)) {
            mLiveData.postValue(BleResult(BleStatus.STOP_SCAN_DEVICE))

            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(mScanCallback)
                } else {
                    mActivity.getBluetoothAdapter()?.stopLeScan(mLeScanCallback)
                }
            }
        }
    }

    override fun close(command: CloseCommand) {
        super.close(command)
        stopScan(StopScanCommand())
    }

}