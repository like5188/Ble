package com.like.ble.state

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.CloseCommand
import com.like.ble.command.StartScanCommand
import com.like.ble.command.StopScanCommand
import com.like.ble.utils.getBluetoothAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙扫描状态
 * 可以进行扫描操作
 */
class ScanState : State() {
    private val mScanning = AtomicBoolean(false)
    private var mStartScanCommand: StartScanCommand? = null
    private var mDelayJob: Job? = null
    private val mScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            mStartScanCommand?.success(result.device, result.rssi, result.scanRecord?.bytes)
        }

        override fun onScanFailed(errorCode: Int) {
            mDelayJob?.cancel()
            mStartScanCommand?.failureAndComplete("错误码：$errorCode")
        }
    }
    private val mLeScanCallback: BluetoothAdapter.LeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        mStartScanCommand?.success(device, rssi, scanRecord)
    }

    override fun startScan(command: StartScanCommand) {
        if (mScanning.compareAndSet(false, true)) {
            mStartScanCommand = command
            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                launch(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(mScanCallback)
                    } else {
                        if (mActivity.getBluetoothAdapter()?.startLeScan(mLeScanCallback) != true) {
                            mDelayJob?.cancel()
                            command.failureAndComplete("扫描失败")
                        }
                    }
                }

                mDelayJob = launch(Dispatchers.IO) {
                    // 在指定超时时间时取消扫描，然后一次扫描就完成了。
                    delay(command.timeout)
                    if (mScanning.get()) {
                        stopScan(StopScanCommand())
                    }
                    command.complete("扫描超时时间到了")
                }
            }
        } else {
            command.failureAndComplete("正在扫描中")
        }
    }

    override fun stopScan(command: StopScanCommand) {
        if (mScanning.compareAndSet(true, false)) {
            mDelayJob?.cancel()
            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(mScanCallback)
                } else {
                    mActivity.getBluetoothAdapter()?.stopLeScan(mLeScanCallback)
                }
                command.successAndComplete()
            }
        } else {
            command.failureAndComplete("扫描已经停止")
        }
    }

    override fun close(command: CloseCommand) {
        mDelayJob?.cancel()
        stopScan(StopScanCommand())
        mStartScanCommand?.complete("主动停止扫描")
        mStartScanCommand = null
        command.successAndComplete()
    }

}