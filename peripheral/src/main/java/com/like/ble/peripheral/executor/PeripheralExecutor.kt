package com.like.ble.peripheral.executor

import androidx.fragment.app.FragmentActivity
import com.like.ble.command.CloseCommand
import com.like.ble.command.Command
import com.like.ble.executor.IExecutor
import com.like.ble.invoker.IInvoker
import com.like.ble.invoker.Invoker
import com.like.ble.peripheral.state.AdvertisingState
import com.like.ble.state.IState

/**
 * 蓝牙外围设备相关命令的执行者。
 */
class PeripheralExecutor(private val mActivity: FragmentActivity) : IExecutor {
    private val mInvoker: IInvoker by lazy { Invoker(mActivity) }
    private val mAdvertisingState: IState by lazy { AdvertisingState(mActivity) }

    override fun execute(command: Command) {
        command.mState = mAdvertisingState
        mInvoker.addCommand(command)
    }

    override fun close() {
        mInvoker.close()
        mAdvertisingState.close(CloseCommand())
    }

}
