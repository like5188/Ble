package com.like.ble.invoker

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.command.*
import com.like.ble.model.BleResult
import com.like.ble.receiver.StateWrapper
import com.like.ble.receiver.state.AdvertisingState
import com.like.ble.receiver.state.ConnectState
import com.like.ble.receiver.state.InitialState
import com.like.ble.receiver.state.ScanState

class Invoker(
    private val mActivity: FragmentActivity,
    private val mLiveData: MutableLiveData<BleResult>
) {
    private var mState: StateWrapper = StateWrapper(mActivity, mLiveData)
    var mCommand: ICommand? = null

    /**
     * 初始化蓝牙
     */
    fun init() {
        mCommand?.let {
            if (it is InitCommand) {
                updateState<InitialState>()
                it.mReceiver = mState
                it.execute()
            }
        }
    }

    /**
     * 开始广播
     */
    fun startAdvertising() {
        mCommand?.let {
            if (it is StartAdvertisingCommand) {
                updateState<AdvertisingState>()
                it.mReceiver = mState
                it.execute()
            }
        }
    }

    /**
     * 停止广播
     */
    fun stopAdvertising() {
        mCommand?.let {
            if (it is StopAdvertisingCommand) {
                updateState<AdvertisingState>()
                it.mReceiver = mState
                it.execute()
            }
        }
    }

    /**
     * 开始扫描设备
     */
    fun startScan() {
        mCommand?.let {
            if (it is StartScanCommand) {
                updateState<ScanState>()
                it.mReceiver = mState
                it.execute()
            }
        }
    }

    /**
     * 停止扫描设备
     */
    fun stopScan() {
        mCommand?.let {
            if (it is StopScanCommand) {
                updateState<ScanState>()
                it.mReceiver = mState
                it.execute()
            }
        }
    }

    /**
     *  连接指定蓝牙设备
     */
    fun connect() {
        mCommand?.let {
            if (it is ConnectCommand) {
                updateState<ConnectState>()
                it.mReceiver = mState
                it.execute()
            }
        }
    }

    /**
     * 断开指定蓝牙设备
     */
    fun disconnect() {
        mCommand?.let {
            if (it is DisconnectCommand) {
                updateState<ConnectState>()
                it.mReceiver = mState
                it.execute()
            }
        }
    }

    /**
     * 读数据
     */
    fun read() {
        mCommand?.let {
            if (it is ReadCommand) {
                updateState<ConnectState>()
                it.mReceiver = mState
                it.execute()
            }
        }
    }

    /**
     * 写数据
     */
    fun write() {
        mCommand?.let {
            if (it is WriteCommand) {
                updateState<ConnectState>()
                it.mReceiver = mState
                it.execute()
            }
        }
    }

    /**
     * 设置mtu
     */
    fun setMtu() {
        mCommand?.let {
            if (it is SetMtuCommand) {
                updateState<ConnectState>()
                it.mReceiver = mState
                it.execute()
            }
        }
    }

    /**
     * 释放资源
     */
    fun close() {
        mCommand?.let {
            if (it is CloseCommand) {
                it.mReceiver = mState
                it.execute()
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