package com.like.ble.receiver

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.command.*
import com.like.ble.model.BleResult
import com.like.ble.model.BleStatus
import com.like.ble.utils.isBluetoothEnable
import com.like.ble.utils.isSupportBluetooth

class StateWrapper(
    private val mActivity: FragmentActivity,
    private val mLiveData: MutableLiveData<BleResult>
) : IState {
    var mState: IState? = null

    override fun init(command: InitCommand) {
        if (!checkSupport()) return
        mState?.init(command)
    }

    override fun startAdvertising(command: StartAdvertisingCommand) {
        if (!checkEnable()) return
        mState?.startAdvertising(command)
    }

    override fun stopAdvertising(command: StopAdvertisingCommand) {
        if (!checkEnable()) return
        mState?.stopAdvertising(command)
    }

    override fun startScan(command: StartScanCommand) {
        if (!checkEnable()) return
        mState?.startScan(command)
    }

    override fun stopScan(command: StopScanCommand) {
        if (!checkEnable()) return
        mState?.stopScan(command)
    }

    override fun connect(command: ConnectCommand) {
        if (!checkEnable()) return
        mState?.connect(command)
    }

    override fun disconnect(command: DisconnectCommand) {
        if (!checkEnable()) return
        mState?.disconnect(command)
    }

    override fun read(command: ReadCommand) {
        if (!checkEnable()) return
        mState?.read(command)
    }

    override fun write(command: WriteCommand) {
        if (!checkEnable()) return
        mState?.write(command)
    }

    override fun setMtu(command: SetMtuCommand) {
        if (!checkEnable()) return
        mState?.setMtu(command)
    }

    override fun close(command: CloseCommand) {
        if (!checkEnable()) return
        mState?.close(command)
    }

    private fun checkSupport(): Boolean {
        mState ?: return false
        if (!mActivity.isSupportBluetooth()) {
            mLiveData.postValue(BleResult(BleStatus.INIT_FAILURE, errorMsg = "phone does not support Bluetooth"))
            return false
        }
        return true
    }

    private fun checkEnable(): Boolean {
        mState ?: return false
        if (!mActivity.isBluetoothEnable()) {
            mLiveData.postValue(BleResult(BleStatus.INIT_FAILURE))
            return false
        }
        return true
    }
}