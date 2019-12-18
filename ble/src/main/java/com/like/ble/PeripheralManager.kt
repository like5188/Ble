package com.like.ble

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.Command
import com.like.ble.executor.IExecutor
import com.like.ble.executor.PeripheralExecutor
import kotlinx.coroutines.launch

/**
 * 蓝牙外围设备管理，使用者直接使用此接口进行相关操作。
 *
 * 一般指非常小或者低功耗设备,更强大的中心设备可以连接外围设备为中心设备提供数据。外设会不停的向外广播，让中心设备知道它的存在。 例如小米手环。
 */
class PeripheralManager(private val mActivity: FragmentActivity) : IBleManager {
    private val mExecutor: IExecutor by lazy { PeripheralExecutor(mActivity) }

    override fun sendCommand(command: Command) {
        if (command.hasGroup(Command.GROUP_PERIPHERAL) || command.hasGroup(Command.GROUP_CLOSE)) {
            mActivity.lifecycleScope.launch {
                mExecutor.execute(command)
            }
        } else {
            Log.e("CentralManager", "蓝牙外围设备不支持该命令：$command")
        }
    }

    /**
     * 关闭所有资源
     */
    override fun close() {
        mExecutor.close()
    }

}