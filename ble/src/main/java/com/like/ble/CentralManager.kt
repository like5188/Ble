package com.like.ble

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.base.Command
import com.like.ble.executor.CentralExecutor
import com.like.ble.executor.IExecutor
import kotlinx.coroutines.launch

/**
 * 蓝牙中心设备管理，使用者直接使用此接口进行相关操作。
 *
 * 可以扫描并连接多个外围设备,从外设中获取信息。
 */
class CentralManager(private val mActivity: FragmentActivity) : IBleManager {
    private val mExecutor: IExecutor by lazy { CentralExecutor(mActivity) }

    override fun sendCommand(command: Command) {
        mActivity.lifecycleScope.launch {
            mExecutor.execute(command)
        }
    }

    /**
     * 关闭所有资源
     */
    override fun close() {
        mExecutor.close()
    }

}