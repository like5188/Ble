package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import com.like.ble.invoker.CentralInvoker
import com.like.ble.command.Command
import com.like.ble.command.concrete.*
import com.like.ble.state.concrete.ConnectState
import com.like.ble.state.concrete.ScanState

/**
 * 蓝牙中心设备状态管理，并用[CentralInvoker]来执行对应的命令。
 */
class CentralStateManager(private val mActivity: FragmentActivity) {
    private val mInvoker: CentralInvoker by lazy {
        CentralInvoker(
            mActivity
        )
    }
    private var mCurState: State? = null
    private val mScanState: ScanState by lazy {
        ScanState().also { it.mActivity = mActivity }
    }
    private val mConnectStateMap = mutableMapOf<String, ConnectState>()

    suspend fun execute(command: Command) {
        val state = getStateByCommand(command)
        if (state == null) {
            command.failureAndComplete("更新蓝牙状态失败")
            return
        }
        mCurState = state
        command.mReceiver = state
        mInvoker.addCommand(command)
    }

    private fun getStateByCommand(command: Command): State? {
        return when (command) {
            is StartScanCommand, is StopScanCommand -> {
                mScanState
            }
            is ConnectCommand -> {
                getConnectStateByAddress(command.address)
            }
            is DisconnectCommand -> {
                getConnectStateByAddress(command.address)
            }
            is ReadCharacteristicCommand -> {
                getConnectStateByAddress(command.address)
            }
            is WriteCharacteristicCommand -> {
                getConnectStateByAddress(command.address)
            }
            is SetMtuCommand -> {
                getConnectStateByAddress(command.address)
            }
            is CloseCommand -> {
                mCurState
            }
            else -> null
        }
    }

    private fun getConnectStateByAddress(address: String): ConnectState? {
        if (!mConnectStateMap.containsKey(address)) {
            mConnectStateMap[address] = ConnectState().also { it.mActivity = mActivity }
        }
        return mConnectStateMap[address]
    }

    fun close() {
        mInvoker.close()
        mScanState.close(CloseCommand())
        mConnectStateMap.forEach {
            it.value.close(CloseCommand())
        }
        mConnectStateMap.clear()
        mCurState = null
    }

}