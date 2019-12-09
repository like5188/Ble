package com.like.ble.state

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import com.like.ble.model.*
import com.like.ble.scanstrategy.IScanStrategy

/**
 * 蓝牙状态
 */
abstract class BaseBleState {
    /**
     * 初始化蓝牙
     */
    open fun init() {}

    /**
     * 开始广播
     */
    open fun startAdvertising(
        serviceUuidString: String,
        readCharUuidString: String,
        writeCharUuidString: String,
        descriptorUuidString: String,
        bluetoothGattServerCallback: BluetoothGattServerCallback
    ) {
    }

    /**
     * 停止广播
     */
    open fun stopAdvertising() {}

    open fun getBluetoothGattServer(): BluetoothGattServer? {
        return null
    }

    /**
     * 开始扫描设备
     */
    open fun startScan(scanStrategy: IScanStrategy, scanTimeout: Long) {}

    /**
     * 停止扫描设备
     */
    open fun stopScan() {}

    /**
     *  连接指定蓝牙设备
     */
    open fun connect(command: BleConnectCommand) {}

    /**
     * 断开指定蓝牙设备
     */
    open fun disconnect(command: BleDisconnectCommand) {}

    /**
     * 读数据
     */
    open fun read(command: BleReadCharacteristicCommand) {}

    /**
     * 写数据
     */
    open fun write(command: BleWriteCharacteristicCommand) {}

    /**
     * 设置mtu
     */
    open fun setMtu(command: BleSetMtuCommand) {}

    /**
     * 释放资源
     */
    open fun close() {}

    /**
     * 获取 BluetoothAdapter
     */
    internal open fun getBluetoothAdapter(): BluetoothAdapter? {
        return null
    }

    internal open fun getBluetoothManager(): BluetoothManager? {
        return null
    }
}