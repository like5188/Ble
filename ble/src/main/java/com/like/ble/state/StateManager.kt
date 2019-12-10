package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.command.*
import com.like.ble.model.BleResult

/**
 * 蓝牙状态管理
 */
class StateManager(
    private val mActivity: FragmentActivity,
    private val mLiveData: MutableLiveData<BleResult>
) {
    private val mState: StateWrapper by lazy { StateWrapper(mActivity, mLiveData) }

    fun update(command: ICommand): IState {
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
            is ReadCommand -> {
                updateState<ConnectState>()
            }
            is WriteCommand -> {
                updateState<ConnectState>()
            }
            is SetMtuCommand -> {
                updateState<ConnectState>()
            }
            is CloseCommand -> {
            }
            else -> {
            }
        }
        return mState
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