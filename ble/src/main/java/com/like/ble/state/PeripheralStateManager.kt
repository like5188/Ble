package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import com.like.ble.command.Command
import com.like.ble.invoker.PeripheralInvoker
import com.like.ble.command.concrete.CloseCommand
import com.like.ble.state.concrete.AdvertisingState

/**
 * 蓝牙外围设备状态管理，并用[com.like.ble.command.PeripheralInvoker]来执行对应的命令。
 */
class PeripheralStateManager(private val mActivity: FragmentActivity) {
    private val mInvoker: PeripheralInvoker by lazy {
        PeripheralInvoker(
            mActivity
        )
    }
    private val mState: AdvertisingState by lazy {
        AdvertisingState().also { it.mActivity = mActivity }
    }

    suspend fun execute(command: Command) {
        command.mReceiver = mState
        mInvoker.addCommand(command)
    }

    fun close() {
        mInvoker.close()
        mState.close(CloseCommand())
    }

}