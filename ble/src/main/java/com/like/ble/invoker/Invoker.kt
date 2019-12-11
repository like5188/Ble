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

    fun init() {
        mInitCommand?.execute()
    }

    fun startAdvertising() {
        mStartAdvertisingCommand?.execute()
    }

    fun stopAdvertising() {
        mStopAdvertisingCommand?.execute()
    }

    fun startScan() {
        mStartScanCommand?.execute()
    }

    fun stopScan() {
        mStopScanCommand?.execute()
    }

    fun connect() {
        mConnectCommand?.execute()
    }

    fun disconnect() {
        mDisconnectCommand?.execute()
    }

    fun readCharacteristic() {
        mReadCommand?.execute()
    }

    fun writeCharacteristic() {
        mWriteCommand?.execute()
    }

    fun setMtu() {
        mSetMtuCommand?.execute()
    }

    fun close() {
        mCloseCommand?.execute()
    }

}