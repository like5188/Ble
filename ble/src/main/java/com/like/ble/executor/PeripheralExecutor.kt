package com.like.ble.executor

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import com.like.ble.command.CloseCommand
import com.like.ble.command.base.Command
import com.like.ble.invoker.Invoker
import com.like.ble.state.AdvertisingState

/**
 * 蓝牙外围设备相关命令的执行者。
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class PeripheralExecutor(private val mActivity: FragmentActivity) : IExecutor {
    private val mInvoker: Invoker by lazy { Invoker(mActivity) }
    private val mState: AdvertisingState by lazy { AdvertisingState(mActivity) }

    override fun execute(command: Command) {
        command.mReceiver = mState
        mInvoker.addCommand(command)
    }

    override fun close() {
        mInvoker.close()
        mState.close(CloseCommand())
    }

}