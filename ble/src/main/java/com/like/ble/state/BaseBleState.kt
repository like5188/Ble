package com.like.ble.state

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.model.*
import com.like.ble.scanstrategy.IScanStrategy
import com.like.ble.utils.isBluetoothEnable
import com.like.ble.utils.isSupportBluetooth

/**
 * 蓝牙状态
 */
abstract class BaseBleState(
    protected val mActivity: FragmentActivity,
    protected val mBleResultLiveData: MutableLiveData<BleResult>
) {
    /**
     * 初始化蓝牙
     */
    fun init() {
        if (!mActivity.isSupportBluetooth()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "phone does not support Bluetooth"))
            return
        }
        onInit()
    }

    open fun onInit() {}

    /**
     * 开始广播
     */
    fun startAdvertising(settings: AdvertiseSettings, advertiseData: AdvertiseData, scanResponse: AdvertiseData) {
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return
        }
        onStartAdvertising(settings, advertiseData, scanResponse)
    }

    open fun onStartAdvertising(settings: AdvertiseSettings, advertiseData: AdvertiseData, scanResponse: AdvertiseData) {}

    /**
     * 停止广播
     */
    fun stopAdvertising() {
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return
        }
        onStopAdvertising()
    }

    open fun onStopAdvertising() {}

    /**
     * 开始扫描设备
     */
    fun startScan(scanStrategy: IScanStrategy, scanTimeout: Long) {
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return
        }
        onStartScan(scanStrategy, scanTimeout)
    }

    open fun onStartScan(scanStrategy: IScanStrategy, scanTimeout: Long) {}

    /**
     * 停止扫描设备
     */
    fun stopScan() {
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return
        }
        onStopScan()
    }

    open fun onStopScan() {}

    /**
     *  连接指定蓝牙设备
     */
    fun connect(command: BleConnectCommand) {
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return
        }
        onConnect(command)
    }

    open fun onConnect(command: BleConnectCommand) {}

    /**
     * 断开指定蓝牙设备
     */
    fun disconnect(command: BleDisconnectCommand) {
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return
        }
        onDisconnect(command)
    }

    open fun onDisconnect(command: BleDisconnectCommand) {}

    /**
     * 读数据
     */
    fun read(command: BleReadCharacteristicCommand) {
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return
        }
        onRead(command)
    }

    open fun onRead(command: BleReadCharacteristicCommand) {}

    /**
     * 写数据
     */
    fun write(command: BleWriteCharacteristicCommand) {
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return
        }
        onWrite(command)
    }

    open fun onWrite(command: BleWriteCharacteristicCommand) {}

    /**
     * 设置mtu
     */
    fun setMtu(command: BleSetMtuCommand) {
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return
        }
        onSetMtu(command)
    }

    open fun onSetMtu(command: BleSetMtuCommand) {}

    /**
     * 释放资源
     */
    fun close() {
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return
        }
        onClose()
    }

    open fun onClose() {}
}