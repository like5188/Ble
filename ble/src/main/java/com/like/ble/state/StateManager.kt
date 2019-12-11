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

    fun updateStateAndExecute(command: Command) {
        command.mReceiver = mState
        when (command) {
            is InitCommand -> {
                updateState<InitialState>()
                mInvoker.mInitCommand = command
                mInvoker.init()
            }
            is StartAdvertisingCommand -> {
                updateState<AdvertisingState>()
                mInvoker.mStartAdvertisingCommand = command
                mInvoker.startAdvertising()
            }
            is StopAdvertisingCommand -> {
                updateState<AdvertisingState>()
                mInvoker.mStopAdvertisingCommand = command
                mInvoker.stopAdvertising()
            }
            is StartScanCommand -> {
                updateState<ScanState>()
                mInvoker.mStartScanCommand = command
                mInvoker.startScan()
            }
            is StopScanCommand -> {
                updateState<ScanState>()
                mInvoker.mStopScanCommand = command
                mInvoker.stopScan()
            }
            is ConnectCommand -> {
                updateState<ConnectState>()
                mInvoker.mConnectCommand = command
                mInvoker.connect()
            }
            is DisconnectCommand -> {
                updateState<ConnectState>()
                mInvoker.mDisconnectCommand = command
                mInvoker.disconnect()
            }
            is ReadCharacteristicCommand -> {
                updateState<ConnectState>()
                mInvoker.mReadCommand = command
                mInvoker.readCharacteristic()
            }
            is WriteCharacteristicCommand -> {
                updateState<ConnectState>()
                mInvoker.mWriteCommand = command
                mInvoker.writeCharacteristic()
            }
            is SetMtuCommand -> {
                updateState<ConnectState>()
                mInvoker.mSetMtuCommand = command
                mInvoker.setMtu()
            }
            is CloseCommand -> {
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