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
    private val mConnectState: ConnectState by lazy {
        ConnectState().also {
            it.mActivity = mActivity
        }
    }

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
                if (mState != mInitialState) {
                    mState?.close(CloseCommand())
                    mState = mInitialState
                }
            }
            is StartAdvertisingCommand, is StopAdvertisingCommand -> {
                if (mState != mAdvertisingState) {
                    mState?.close(CloseCommand())
                    mState = mAdvertisingState
                }
            }
            is StartScanCommand, is StopScanCommand -> {
                if (mState != mScanState) {
                    mState?.close(CloseCommand())
                    mState = mScanState
                }
            }
            is ConnectCommand, is DisconnectCommand, is ReadCharacteristicCommand, is WriteCharacteristicCommand, is SetMtuCommand -> {
                if (mState != mConnectState) {
                    mState?.close(CloseCommand())
                    mState = mConnectState
                }
            }
            is CloseCommand -> {
            }
        }
    }

}