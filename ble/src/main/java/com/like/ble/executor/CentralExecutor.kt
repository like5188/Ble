package com.like.ble.executor

import androidx.fragment.app.FragmentActivity
import com.like.ble.command.*
import com.like.ble.invoker.CentralInvoker
import com.like.ble.invoker.Invoker
import com.like.ble.state.State
import com.like.ble.state.ConnectState
import com.like.ble.state.ScanState

/**
 * 蓝牙中心设备相关命令的执行者。
 */
class CentralExecutor(private val mActivity: FragmentActivity) : IExecutor {
    private val mInvoker: Invoker by lazy { CentralInvoker(mActivity) }
    private var mCurState: State? = null
    private val mScanState: ScanState by lazy {
        ScanState().also { it.mActivity = mActivity }
    }
    private val mConnectStateMap = mutableMapOf<String, ConnectState>()

    override suspend fun execute(command: Command) {
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

    override fun close() {
        mInvoker.close()
        mScanState.close(CloseCommand())
        mConnectStateMap.forEach {
            it.value.close(CloseCommand())
        }
        mConnectStateMap.clear()
        mCurState = null
    }

}