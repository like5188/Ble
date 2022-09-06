package com.like.ble.peripheral.executor

import androidx.activity.ComponentActivity
import com.like.ble.command.CloseCommand
import com.like.ble.command.Command
import com.like.ble.executor.CommandExecutor
import com.like.ble.peripheral.state.AdvertisingState
import com.like.ble.state.IState

/**
 * 蓝牙外围设备相关命令的执行者。
 */
class PeripheralExecutor(activity: ComponentActivity) : CommandExecutor(activity) {
    private val mAdvertisingState: IState by lazy { AdvertisingState(mActivity) }

    override fun onExecute(command: Command): Boolean {
        command.mState = mAdvertisingState
        return true
    }

    override fun onClose() {
        mAdvertisingState.close(CloseCommand())
    }

}
