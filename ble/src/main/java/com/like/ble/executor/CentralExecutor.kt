package com.like.ble.executor

import androidx.fragment.app.FragmentActivity
import com.like.ble.command.CloseCommand
import com.like.ble.command.Command
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
    private val mScanState: ScanState by lazy {
        ScanState().also { it.mActivity = mActivity }
    }
    private val mConnectStateMap = mutableMapOf<String, ConnectState>()

    override suspend fun execute(command: Command) {
        val state = getStateByCommand(command)
        if (state == null) {
            command.failureAndComplete("更新蓝牙状态失败，无法执行命令：$command")
            return
        }
        mCurState = state
        command.mReceiver = state
        mInvoker.addCommand(command)
    }

    private fun getStateByCommand(command: Command): State? {
        return when {
            command.hasGroup(Command.GROUP_CENTRAL_SCAN) -> {
                mScanState
            }
            command.hasGroup(Command.GROUP_CENTRAL_DEVICE) -> {
                if (!mConnectStateMap.containsKey(command.address)) {
                    mConnectStateMap[command.address] = ConnectState().also { it.mActivity = mActivity }
                }
                mConnectStateMap[command.address]
            }
            command.hasGroup(Command.GROUP_CLOSE) -> {
                mCurState
            }
            else -> null
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