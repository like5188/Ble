package com.like.ble.central.executor

import androidx.activity.ComponentActivity
import com.like.ble.central.command.AddressCommand
import com.like.ble.central.command.StartScanCommand
import com.like.ble.central.command.StopScanCommand
import com.like.ble.central.state.ConnectState
import com.like.ble.central.state.ScanState
import com.like.ble.command.CloseCommand
import com.like.ble.command.Command
import com.like.ble.executor.IExecutor
import com.like.ble.invoker.IInvoker
import com.like.ble.invoker.Invoker
import com.like.ble.state.IState

/**
 * 蓝牙中心设备相关命令的执行者。
 */
class CentralExecutor(private val mActivity: ComponentActivity) : IExecutor {
    private val mInvoker: IInvoker by lazy { Invoker(mActivity) }
    private var mCurState: IState? = null
    private val mScanState: IState by lazy { ScanState(mActivity) }
    private val mConnectStateMap = mutableMapOf<String, IState>()

    override fun execute(command: Command) {
        val state = getStateByCommand(command)
        if (state == null) {
            command.errorAndComplete("更新蓝牙状态失败，无法执行命令：$command")
            return
        }
        mCurState = state
        command.mState = state
        mInvoker.addCommand(command)
    }

    private fun getStateByCommand(command: Command): IState? {
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