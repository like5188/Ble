package com.like.ble.state

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import com.like.ble.model.*
import com.like.ble.scanstrategy.IScanStrategy

/**
 * 蓝牙状态
 */
interface IBleState {

    /**
     * 初始化蓝牙
     */
    fun init()

    /**
     * 开始广播
     */
    fun startAdvertising(settings: AdvertiseSettings, advertiseData: AdvertiseData, scanResponse: AdvertiseData)

    /**
     * 停止广播
     */
    fun stopAdvertising()

    /**
     * 开始扫描设备
     */
    fun startScan(scanStrategy: IScanStrategy, scanTimeout: Long)

    /**
     * 停止扫描设备
     */
    fun stopScan()

    /**
     *  连接指定蓝牙设备
     */
    fun connect(command: BleConnectCommand)

    /**
     * 断开指定蓝牙设备
     */
    fun disconnect(command: BleDisconnectCommand)

    /**
     * 读数据
     */
    fun read(command: BleReadCharacteristicCommand)

    /**
     * 写数据
     */
    fun write(command: BleWriteCharacteristicCommand)

    /**
     * 设置mtu
     */
    fun setMtu(command: BleSetMtuCommand)

    /**
     * 释放资源
     */
    fun close()
}