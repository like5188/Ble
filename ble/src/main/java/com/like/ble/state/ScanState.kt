package com.like.ble.state

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.CloseCommand
import com.like.ble.command.StartScanCommand
import com.like.ble.command.StopScanCommand
import com.like.ble.utils.getBluetoothAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙扫描状态
 * 可以进行扫描操作
 */
class ScanState : State() {
    private val mScanning = AtomicBoolean(false)
    private val mScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            filterScanResult(result.device, result.rssi, result.scanRecord?.bytes)
        }

        override fun onScanFailed(errorCode: Int) {
            val curCommand = mCurCommand
            if (curCommand is StartScanCommand) {
                curCommand.failureAndComplete("错误码：$errorCode")
            }
        }
    }
    private val mLeScanCallback: BluetoothAdapter.LeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        filterScanResult(device, rssi, scanRecord)
    }

    @Synchronized
    private fun filterScanResult(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {
        val curCommand = mCurCommand
        if (curCommand is StartScanCommand) {
            Log.e("ScanState", "deviceName=${device.name} deviceAddress=${device.address} serviceUuids=${Arrays.toString(device.uuids)}")
            // 设备名字匹配
            if (curCommand.filterDeviceName.isNotEmpty()) {
                val deviceName = device.name ?: ""
                if (curCommand.fuzzyMatchingDeviceName) {// 模糊匹配
                    if (!deviceName.contains(curCommand.filterDeviceName)) {
                        return
                    }
                } else {
                    if (deviceName != curCommand.filterDeviceName) {
                        return
                    }
                }
            }
            // 设备地址匹配
            if (curCommand.filterDeviceAddress.isNotEmpty() && device.address != curCommand.filterDeviceAddress) {
                return
            }
            curCommand.success(device, rssi, scanRecord)
        }
    }

    override fun startScan(command: StartScanCommand) {
        mCurCommand = command
        if (mScanning.compareAndSet(false, true)) {
            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                launch(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (command.filterServiceUuid == null) {
                            mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(mScanCallback)
                        } else {
                            mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(
                                listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(command.filterServiceUuid)).build()),
                                ScanSettings.Builder().build(),
                                mScanCallback
                            )
                        }
                    } else {
                        if (command.filterServiceUuid == null) {
                            if (mActivity.getBluetoothAdapter()?.startLeScan(mLeScanCallback) != true) {
                                command.failureAndComplete("扫描失败")
                            }
                        } else {
                            if (mActivity.getBluetoothAdapter()?.startLeScan(arrayOf(command.filterServiceUuid), mLeScanCallback) != true) {
                                command.failureAndComplete("扫描失败")
                            }
                        }
                    }
                }

                command.addJob(launch(Dispatchers.IO) {
                    // 在指定超时时间时取消扫描，然后一次扫描就完成了。
                    delay(command.timeout)
                    if (mScanning.get()) {
                        stopScan(StopScanCommand())
                    }
                    command.complete("扫描超时时间到了")
                })
            }
        } else {
            command.failureAndComplete("正在扫描中")
        }
    }

    override fun stopScan(command: StopScanCommand) {
        if (mScanning.compareAndSet(true, false)) {
            val curCommand = mCurCommand
            if (curCommand is StartScanCommand) {
                curCommand.complete("主动停止扫描")
            }

            mCurCommand = command

            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(mScanCallback)
                } else {
                    mActivity.getBluetoothAdapter()?.stopLeScan(mLeScanCallback)
                }
                command.successAndComplete()
            }
        } else {
            mCurCommand = command
            command.failureAndComplete("扫描已经停止")
        }
    }

    override fun close(command: CloseCommand) {
        stopScan(StopScanCommand())
        mCurCommand = null
        command.successAndComplete()
    }

}