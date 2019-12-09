package com.like.ble.state

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import com.like.ble.model.*
import com.like.ble.scanstrategy.IScanStrategy

/**
 * 蓝牙状态
 */
abstract class BleStateAdapter : IBleState {
    override fun init() {
    }

    override fun startAdvertising(settings: AdvertiseSettings, advertiseData: AdvertiseData, scanResponse: AdvertiseData) {
    }

    override fun stopAdvertising() {
    }

    override fun startScan(scanStrategy: IScanStrategy, scanTimeout: Long) {
    }

    override fun stopScan() {
    }

    override fun connect(command: BleConnectCommand) {
    }

    override fun disconnect(command: BleDisconnectCommand) {
    }

    override fun read(command: BleReadCharacteristicCommand) {
    }

    override fun write(command: BleWriteCharacteristicCommand) {
    }

    override fun setMtu(command: BleSetMtuCommand) {
    }

    override fun close() {
    }

}