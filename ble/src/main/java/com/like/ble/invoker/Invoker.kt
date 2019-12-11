package com.like.ble.invoker

import com.like.ble.command.Command

class Invoker {
    var mInitCommand: Command? = null
    var mStartAdvertisingCommand: Command? = null
    var mStopAdvertisingCommand: Command? = null
    var mStartScanCommand: Command? = null
    var mStopScanCommand: Command? = null
    var mConnectCommand: Command? = null
    var mDisconnectCommand: Command? = null
    var mReadCommand: Command? = null
    var mWriteCommand: Command? = null
    var mSetMtuCommand: Command? = null
    var mCloseCommand: Command? = null

    /**
     * 初始化蓝牙
     */
    fun init() {
        mInitCommand?.execute()
    }

    /**
     * 开始广播
     */
    fun startAdvertising() {
        mStartAdvertisingCommand?.execute()
    }

    /**
     * 停止广播
     */
    fun stopAdvertising() {
        mStopAdvertisingCommand?.execute()
    }

    /**
     * 开始扫描设备
     */
    fun startScan() {
        mStartScanCommand?.execute()
    }

    /**
     * 停止扫描设备
     */
    fun stopScan() {
        mStopScanCommand?.execute()
    }

    /**
     *  连接指定蓝牙设备
     */
    fun connect() {
        mConnectCommand?.execute()
    }

    /**
     * 断开指定蓝牙设备
     */
    fun disconnect() {
        mDisconnectCommand?.execute()
    }

    /**
     * 读数据
     */
    fun read() {
        mReadCommand?.execute()
    }

    /**
     * 写数据
     */
    fun write() {
        mWriteCommand?.execute()
    }

    /**
     * 设置mtu
     */
    fun setMtu() {
        mSetMtuCommand?.execute()
    }

    /**
     * 释放资源
     */
    fun close() {
        mCloseCommand?.execute()
    }

}