package com.like.ble.state

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.model.*
import com.like.ble.utils.isBluetoothEnable
import com.like.ble.utils.isSupportBluetooth

/**
 * 蓝牙状态
 */
class BleStateWrapper(
    private val mActivity: FragmentActivity,
    private val mBleResultLiveData: MutableLiveData<BleResult>
) : IBleState {
    var mBleState: IBleState? = null

    override fun init() {
        if (!checkSupport()) return
        mBleState?.init()
    }

    override fun startAdvertising(settings: AdvertiseSettings, advertiseData: AdvertiseData, scanResponse: AdvertiseData) {
        if (!checkEnable()) return
        mBleState?.startAdvertising(settings, advertiseData, scanResponse)
    }

    override fun stopAdvertising() {
        if (!checkEnable()) return
        mBleState?.stopAdvertising()
    }

    override fun startScan(command: BleStartScanCommand) {
        if (!checkEnable()) return
        mBleState?.startScan(command)
    }

    override fun stopScan(command: BleStopScanCommand) {
        if (!checkEnable()) return
        mBleState?.stopScan(command)
    }

    override fun connect(command: BleConnectCommand) {
        if (!checkEnable()) return
        mBleState?.connect(command)
    }

    override fun disconnect(command: BleDisconnectCommand) {
        if (!checkEnable()) return
        mBleState?.disconnect(command)
    }

    override fun read(command: BleReadCharacteristicCommand) {
        if (!checkEnable()) return
        mBleState?.read(command)
    }

    override fun write(command: BleWriteCharacteristicCommand) {
        if (!checkEnable()) return
        mBleState?.write(command)
    }

    override fun setMtu(command: BleSetMtuCommand) {
        if (!checkEnable()) return
        mBleState?.setMtu(command)
    }

    override fun close() {
        if (!checkEnable()) return
        mBleState?.close()
        mBleState = null
    }

    private fun checkSupport(): Boolean {
        mBleState ?: return false
        if (!mActivity.isSupportBluetooth()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "phone does not support Bluetooth"))
            return false
        }
        return true
    }

    private fun checkEnable(): Boolean {
        mBleState ?: return false
        if (!mActivity.isBluetoothEnable()) {
            mBleResultLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return false
        }
        return true
    }


}