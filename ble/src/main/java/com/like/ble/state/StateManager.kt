package com.like.ble.state

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.like.ble.command.Command
import com.like.ble.command.CommandInvoker
import com.like.ble.command.concrete.*
import com.like.ble.state.concrete.AdvertisingState
import com.like.ble.state.concrete.ConnectState
import com.like.ble.state.concrete.InitialState
import com.like.ble.state.concrete.ScanState
import com.like.ble.utils.isBluetoothEnable
import com.like.ble.utils.isSupportBluetooth

/**
 * 蓝牙状态管理，并用[CommandInvoker]来执行对应的命令。
 */
class StateManager(private val mActivity: FragmentActivity) {
    private val mCommandInvoker: CommandInvoker by lazy { CommandInvoker() }
    private var mState: State? = null
    private val mInitialState: InitialState by lazy {
        InitialState().also {
            it.mActivity = mActivity
        }
    }
    private val mAdvertisingState: AdvertisingState by lazy {
        AdvertisingState().also {
            it.mActivity = mActivity
        }
    }
    private val mScanState: ScanState by lazy {
        ScanState().also {
            it.mActivity = mActivity
        }
    }
    private val mConnectStateMap = mutableMapOf<String, ConnectState>()

    fun execute(command: Command) {
        when (command) {
            is InitCommand -> {
                if (!mActivity.isSupportBluetooth()) {
                    mActivity.runOnUiThread {
                        Toast.makeText(mActivity, "phone does not support Bluetooth", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
            }
            else -> {
                if (!mActivity.isBluetoothEnable()) {
                    mActivity.runOnUiThread {
                        Toast.makeText(mActivity, "Bluetooth is not enable", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
            }
        }
        updateStateByCommand(command)
        mState?.let {
            command.mReceiver = it
            mCommandInvoker.addCommand(command)
            mCommandInvoker.execute()
        }
    }

    private fun updateStateByCommand(command: Command) {
        when (command) {
            is InitCommand -> {
                if (mState !is InitialState) {
                    closeState(mState)
                    mState = mInitialState
                }
            }
            is StartAdvertisingCommand, is StopAdvertisingCommand -> {
                if (mState !is AdvertisingState) {
                    closeState(mState)
                    mState = mAdvertisingState
                }
            }
            is StartScanCommand, is StopScanCommand -> {
                if (mState !is ScanState) {
                    closeState(mState)
                    mState = mScanState
                }
            }
            is ConnectCommand -> {
                updateConnectStateByAddress(command.address)
            }
            is DisconnectCommand -> {
                updateConnectStateByAddress(command.address)
            }
            is ReadCharacteristicCommand -> {
                updateConnectStateByAddress(command.address)
            }
            is WriteCharacteristicCommand -> {
                updateConnectStateByAddress(command.address)
            }
            is SetMtuCommand -> {
                updateConnectStateByAddress(command.address)
            }
            is CloseCommand -> {
            }
        }
    }

    private fun updateConnectStateByAddress(address: String) {
        if (mState !is ConnectState) {
            closeState(mState)
        }
        if (!mConnectStateMap.containsKey(address)) {
            mConnectStateMap[address] = ConnectState().also { it.mActivity = mActivity }
        }
        mState = mConnectStateMap[address]
    }

    private fun closeState(state: State?) {
        state ?: return
        when (state) {
            is InitialState -> {
                state.close(CloseCommand())
            }
            is AdvertisingState -> {
                state.close(CloseCommand())
            }
            is ScanState -> {
                state.close(CloseCommand())
            }
            is ConnectState -> {
                mConnectStateMap.forEach {
                    it.value.close(CloseCommand())
                }
                mConnectStateMap.clear()
            }
        }
    }

}