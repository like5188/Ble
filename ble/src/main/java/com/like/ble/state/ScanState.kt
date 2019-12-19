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
import androidx.fragment.app.FragmentActivity
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
class ScanState(private val mActivity: FragmentActivity) : State() {
    private val mScanning = AtomicBoolean(false)
    private var mStartScanCommand: StartScanCommand? = null
    private val mScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            filterScanResult(result.device, result.rssi, result.scanRecord?.bytes)
        }

        override fun onScanFailed(errorCode: Int) {
            mStartScanCommand?.failureAndCompleteIfIncomplete("错误码：$errorCode")
            mScanning.set(false)
        }

        // Bluetoothadapter.isOffloadedScanBatchingSupported()</br>
        //    判断当前手机蓝牙芯片是否支持批处理扫描。如果支持扫描则使用批处理扫描，可通过ScanSettings.Builder对象调用setReportDelay(Long)方法来设置蓝牙LE扫描的报告延迟的时间（以毫秒为单位）来启动批处理扫描模式。
        //
        //ScanSettings.Builder.setReportDelay(Long);
        //    当设备蓝牙芯片支持批处理扫描时，用来设置蓝牙LE扫描的报告延迟的时间（以毫秒为单位）。</br>
        //    该参数默认为 0，如果不修改它的值，则默认只会在onScanResult(int,ScanResult)中返回扫描到的蓝牙设备，不会触发不会触发onBatchScanResults(List)方法。
        //    设置为0以立即通知结果,不开启批处理扫描模式。即ScanCallback蓝牙回调中，不会触发onBatchScanResults(List)方法，但会触发onScanResult(int,ScanResult)方法，返回扫描到的蓝牙设备。
        //    当设置的时间大于0L时，则会开启批处理扫描模式。即触发onBatchScanResults(List)方法，返回扫描到的蓝牙设备列表。但不会触发onScanResult(int,ScanResult)方法。</br>
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
        }
    }
    private val mLeScanCallback: BluetoothAdapter.LeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        filterScanResult(device, rssi, scanRecord)
    }

    @Synchronized
    private fun filterScanResult(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {
        mStartScanCommand?.let {
            Log.e("ScanState", "deviceName=${device.name} deviceAddress=${device.address} serviceUuids=${Arrays.toString(device.uuids)}")
            // 设备名字匹配
            if (it.filterDeviceName.isNotEmpty()) {
                val deviceName = device.name ?: ""
                if (it.fuzzyMatchingDeviceName) {// 模糊匹配
                    if (!deviceName.contains(it.filterDeviceName)) {
                        return
                    }
                } else {
                    if (deviceName != it.filterDeviceName) {
                        return
                    }
                }
            }
            // 设备地址匹配
            if (it.filterDeviceAddress.isNotEmpty() && device.address != it.filterDeviceAddress) {
                return
            }
            it.successIfIncomplete(device, rssi, scanRecord)
        }
    }

    @Synchronized
    override fun startScan(command: StartScanCommand) {
        if (mScanning.compareAndSet(false, true)) {
            mStartScanCommand = command
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (command.filterServiceUuid == null) {
                    mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(mScanCallback)
                } else {
                    // serviceUuid 只能在这里过滤，不能放到 filterScanResult() 方法中去，因为只有 gatt.discoverServices() 过后，device.getUuids() 方法才不会返回 null。
                    mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.startScan(
                        listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(command.filterServiceUuid)).build()),
                        // setScanMode() 设置扫描模式。可选择的模式有三种：
                        //     ScanSettings.SCAN_MODE_LOW_POWER 低功耗模式
                        //     ScanSettings.SCAN_MODE_BALANCED 平衡模式
                        //     ScanSettings.SCAN_MODE_LOW_LATENCY 高功耗模式
                        // 从上到下，会越来越耗电,但扫描间隔越来越短，即扫描速度会越来越快。
                        // setCallbackType() 设置回调类型，可选择的类型有三种：
                        //    ScanSettings.CALLBACK_TYPE_ALL_MATCHES 数值: 1 寻找符合过滤条件的蓝牙广播，如果没有设置过滤条件，则返回全部广播包
                        //    ScanSettings.CALLBACK_TYPE_FIRST_MATCH 数值: 2 仅针对与筛选条件匹配的第一个广播包触发结果回调。
                        //    ScanSettings.CALLBACK_TYPE_MATCH_LOST 数值: 4
                        //    回调类型一般设置ScanSettings.CALLBACK_TYPE_ALL_MATCHES，有过滤条件时过滤，返回符合过滤条件的蓝牙广播。无过滤条件时，返回全部蓝牙广播。
                        // setMatchMode() 设置蓝牙LE扫描滤波器硬件匹配的匹配模式，一般设置ScanSettings.MATCH_MODE_STICKY
                        ScanSettings.Builder().build(),
                        mScanCallback
                    )
                }
            } else {
                if (command.filterServiceUuid == null) {
                    if (mActivity.getBluetoothAdapter()?.startLeScan(mLeScanCallback) != true) {
                        command.failureAndCompleteIfIncomplete("扫描失败")
                        return
                    }
                } else {
                    if (mActivity.getBluetoothAdapter()?.startLeScan(arrayOf(command.filterServiceUuid), mLeScanCallback) != true) {
                        command.failureAndCompleteIfIncomplete("扫描失败")
                        return
                    }
                }
            }

            command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
                // 在指定超时时间时取消扫描，然后一次扫描就完成了。
                delay(command.timeout)
                stopScan(StopScanCommand())
            })
        } else {
            command.failureAndCompleteIfIncomplete("正在扫描中")
        }
    }

    @Synchronized
    override fun stopScan(command: StopScanCommand) {
        if (mScanning.compareAndSet(true, false)) {
            mStartScanCommand?.completeIfIncomplete()
            mStartScanCommand = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mActivity.getBluetoothAdapter()?.bluetoothLeScanner?.stopScan(mScanCallback)
            } else {
                mActivity.getBluetoothAdapter()?.stopLeScan(mLeScanCallback)
            }

        }
        command.completeIfIncomplete()
    }

    @Synchronized
    override fun close(command: CloseCommand) {
        stopScan(StopScanCommand())
        command.completeIfIncomplete()
    }

}