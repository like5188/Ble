package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.command.*
import com.like.ble.invoker.Invoker
import com.like.ble.model.BleResult

/**
 * 蓝牙状态管理，并调用[Invoker]的相关方法执行对应的命令。
 */
class StateManager(
    private val mActivity: FragmentActivity,
    private val mLiveData: MutableLiveData<BleResult>
) {
    private val mState: StateWrapper by lazy { StateWrapper(mActivity, mLiveData) }
    private val mInvoker: Invoker by lazy { Invoker() }

    fun updateStateAndExecute(command: ICommand) {
        when (command) {
            is InitCommand -> {
                updateState<InitialState>()
                command.mReceiver = mState
                mInvoker.mInitCommand = command
                mInvoker.init()
            }
            is StartAdvertisingCommand -> {
                updateState<AdvertisingState>()
                command.mReceiver = mState
                mInvoker.mStartAdvertisingCommand = command
                mInvoker.startAdvertising()
            }
            is StopAdvertisingCommand -> {
                updateState<AdvertisingState>()
                command.mReceiver = mState
                mInvoker.mStopAdvertisingCommand = command
                mInvoker.stopAdvertising()
            }
            is StartScanCommand -> {
                updateState<ScanState>()
                command.mReceiver = mState
                mInvoker.mStartScanCommand = command
                mInvoker.startScan()
            }
            is StopScanCommand -> {
                updateState<ScanState>()
                command.mReceiver = mState
                mInvoker.mStopScanCommand = command
                mInvoker.stopScan()
            }
            is ConnectCommand -> {
                updateState<ConnectState>()
                command.mReceiver = mState
                mInvoker.mConnectCommand = command
                mInvoker.connect()
            }
            is DisconnectCommand -> {
                updateState<ConnectState>()
                command.mReceiver = mState
                mInvoker.mDisconnectCommand = command
                mInvoker.disconnect()
            }
            is ReadCommand -> {
                updateState<ConnectState>()
                command.mReceiver = mState
                mInvoker.mReadCommand = command
                mInvoker.read()
            }
            is WriteCommand -> {
                updateState<ConnectState>()
                command.mReceiver = mState
                mInvoker.mWriteCommand = command
                mInvoker.write()
            }
            is SetMtuCommand -> {
                updateState<ConnectState>()
                command.mReceiver = mState
                mInvoker.mSetMtuCommand = command
                mInvoker.setMtu()
            }
            is CloseCommand -> {
                command.mReceiver = mState
                mInvoker.mCloseCommand = command
                mInvoker.close()
            }
        }
    }

    private inline fun <reified T> updateState() {
        when (T::class.java) {
            InitialState::class.java -> {
                if (mState.mState !is InitialState) {
                    mState.mState?.close(CloseCommand())
                    mState.mState = InitialState(mActivity, mLiveData)
                }
            }
            AdvertisingState::class.java -> {
                if (mState.mState !is AdvertisingState) {
                    if (mState.mState !is InitialState) {
                        mState.mState?.close(CloseCommand())
                    }
                    mState.mState = AdvertisingState(mActivity, mLiveData)
                }
            }
            ScanState::class.java -> {
                if (mState.mState !is ScanState) {
                    if (mState.mState !is InitialState) {
                        mState.mState?.close(CloseCommand())
                    }
                    mState.mState = ScanState(mActivity, mLiveData)
                }
            }
            ConnectState::class.java -> {
                if (mState.mState !is ConnectState) {
                    if (mState.mState !is InitialState) {
                        mState.mState?.close(CloseCommand())
                    }
                    mState.mState = ConnectState(mActivity, mLiveData)
                }
            }
        }

    }
}