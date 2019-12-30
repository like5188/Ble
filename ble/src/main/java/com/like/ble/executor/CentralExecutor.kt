package com.like.ble.executor

import androidx.fragment.app.FragmentActivity
import com.like.ble.command.*
import com.like.ble.command.base.AddressCommand
import com.like.ble.command.base.Command
import com.like.ble.invoker.CentralInvoker
import com.like.ble.invoker.Invoker
import com.like.ble.state.ConnectState
import com.like.ble.state.ScanState
import com.like.ble.state.State

/**
 * 蓝牙中心设备相关命令的执行者。
 */
class CentralExecutor(private val mActivity: FragmentActivity) : IExecutor {
    private val mInvoker: Invoker by lazy { CentralInvoker(mActivity) }
    private var mCurState: State? = null
    private val mScanState: ScanState by lazy { ScanState(mActivity) }
    private val mConnectStateMap = mutableMapOf<String, ConnectState>()

    override suspend fun execute(command: Command) {
        val state = getStateByCommand(command)
        if (state == null) {
            command.failureAndCompleteIfIncomplete("更新蓝牙状态失败，无法执行命令：$command")
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
            is AddressCommand -> {
                if (!mConnectStateMap.containsKey(command.address)) {
                    mConnectStateMap[command.address] = ConnectState(mActivity)
                }
                mConnectStateMap[command.address]
            }
            else -> mCurState
        }
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