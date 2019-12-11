package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.command.Command
import com.like.ble.command.CommandInvoker
import com.like.ble.command.concrete.*
import com.like.ble.model.BleResult
import com.like.ble.state.concrete.AdvertisingState
import com.like.ble.state.concrete.ConnectState
import com.like.ble.state.concrete.InitialState
import com.like.ble.state.concrete.ScanState

/**
 * 蓝牙状态管理，并用[CommandInvoker]来执行对应的命令。
 */
class StateManager(
    private val mActivity: FragmentActivity,
    private val mLiveData: MutableLiveData<BleResult>
) {
    private val mCommandInvoker: CommandInvoker by lazy { CommandInvoker() }
    private val mStateWrapper: StateWrapper by lazy {
        StateWrapper().also {
            it.mActivity = mActivity
            it.mLiveData = mLiveData
        }
    }
    private val mInitialState: InitialState by lazy {
        InitialState().also {
            it.mActivity = mActivity
            it.mLiveData = mLiveData
        }
    }
    private val mAdvertisingState: AdvertisingState by lazy {
        AdvertisingState().also {
            it.mActivity = mActivity
            it.mLiveData = mLiveData
        }
    }
    private val mScanState: ScanState by lazy {
        ScanState().also {
            it.mActivity = mActivity
            it.mLiveData = mLiveData
        }
    }
    private val mConnectState: ConnectState by lazy {
        ConnectState().also {
            it.mActivity = mActivity
            it.mLiveData = mLiveData
        }
    }

    fun updateStateAndExecute(command: Command) {
        updateStateByCommand(command)
        command.mReceiver = mStateWrapper
        mCommandInvoker.addCommand(command)
        mCommandInvoker.execute()
    }

    private fun updateStateByCommand(command: Command) {
        when (command) {
            is InitCommand -> {
                updateState<InitialState>()
            }
            is StartAdvertisingCommand -> {
                updateState<AdvertisingState>()
            }
            is StopAdvertisingCommand -> {
                updateState<AdvertisingState>()
            }
            is StartScanCommand -> {
                updateState<ScanState>()
            }
            is StopScanCommand -> {
                updateState<ScanState>()
            }
            is ConnectCommand -> {
                updateState<ConnectState>()
            }
            is DisconnectCommand -> {
                updateState<ConnectState>()
            }
            is ReadCharacteristicCommand -> {
                updateState<ConnectState>()
            }
            is WriteCharacteristicCommand -> {
                updateState<ConnectState>()
            }
            is SetMtuCommand -> {
                updateState<ConnectState>()
            }
            is CloseCommand -> {
            }
        }
    }

    private inline fun <reified T> updateState() {
        when (T::class.java) {
            InitialState::class.java -> {
                if (mStateWrapper.mState != mInitialState) {
                    mStateWrapper.mState?.close(CloseCommand())
                    mStateWrapper.mState = mInitialState
                }
            }
            AdvertisingState::class.java -> {
                if (mStateWrapper.mState != mAdvertisingState) {
                    mStateWrapper.mState?.close(CloseCommand())
                    mStateWrapper.mState = mAdvertisingState
                }
            }
            ScanState::class.java -> {
                if (mStateWrapper.mState != mScanState) {
                    mStateWrapper.mState?.close(CloseCommand())
                    mStateWrapper.mState = mScanState
                }
            }
            ConnectState::class.java -> {
                if (mStateWrapper.mState != mConnectState) {
                    mStateWrapper.mState?.close(CloseCommand())
                    mStateWrapper.mState = mConnectState
                }
            }
        }

    }
}