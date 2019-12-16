package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import com.like.ble.command.*

/**
 * 蓝牙状态基类。
 */
abstract class State {
    lateinit var mActivity: FragmentActivity
    protected var mCurCommand: Command? = null

    open fun startAdvertising(command: StartAdvertisingCommand) {
        mCurCommand = command
    }

    open fun stopAdvertising(command: StopAdvertisingCommand) {
        mCurCommand = command
    }

    open fun startScan(command: StartScanCommand) {
        mCurCommand = command
    }

    open fun stopScan(command: StopScanCommand) {
        mCurCommand = command
    }

    open fun connect(command: ConnectCommand) {
        mCurCommand = command
    }

    open fun disconnect(command: DisconnectCommand) {
        mCurCommand = command
    }

    open fun readCharacteristic(command: ReadCharacteristicCommand) {
        mCurCommand = command
    }

    open fun writeCharacteristic(command: WriteCharacteristicCommand) {
        mCurCommand = command
    }

    open fun setMtu(command: SetMtuCommand) {
        mCurCommand = command
    }

    open fun enableCharacteristicNotify(command: EnableCharacteristicNotifyCommand) {
        mCurCommand = command
    }

    open fun disableCharacteristicNotify(command: DisableCharacteristicNotifyCommand) {
        mCurCommand = command
    }

    open fun enableCharacteristicIndicate(command: EnableCharacteristicIndicateCommand) {
        mCurCommand = command
    }

    open fun disableCharacteristicIndicate(command: DisableCharacteristicIndicateCommand) {
        mCurCommand = command
    }

    open fun writeAndWaitForData(command: WriteAndWaitForDataCommand) {
        mCurCommand = command
    }

    open fun readRemoteRssi(command: ReadRemoteRssiCommand) {
        mCurCommand = command
    }

    open fun requestConnectionPriority(command: RequestConnectionPriorityCommand) {
        mCurCommand = command
    }

    open fun close(command: CloseCommand) {}
}