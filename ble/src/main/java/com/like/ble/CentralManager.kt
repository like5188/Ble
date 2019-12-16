package com.like.ble

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.Command
import com.like.ble.executor.CentralExecutor
import com.like.ble.executor.IExecutor
import kotlinx.coroutines.launch

/**
 * 蓝牙中心设备管理，使用者直接使用此接口进行相关操作。
 */
class CentralManager(private val mActivity: FragmentActivity) : IBleManager {
    private val mExecutor: IExecutor by lazy { CentralExecutor(mActivity) }

    override fun sendCommand(command: Command) {
        if (command.hasGroup(Command.GROUP_CENTRAL) || command.hasGroup(Command.GROUP_CLOSE)) {
            mActivity.lifecycleScope.launch {
                mExecutor.execute(command)
            }
        } else {
            Log.e("CentralManager", "蓝牙中心设备不支持该命令：$command")
        }
    }

    /**
     * 关闭所有资源
     */
    override fun close() {
        mExecutor.close()
    }

}