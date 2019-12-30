package com.like.ble

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.base.Command
import com.like.ble.executor.IExecutor
import com.like.ble.executor.PeripheralExecutor
import kotlinx.coroutines.launch

/**
 * 蓝牙外围设备管理，使用者直接使用此接口进行相关操作。
 *
 * 一般指非常小或者低功耗设备,更强大的中心设备可以连接外围设备为中心设备提供数据。外设会不停的向外广播，让中心设备知道它的存在。 例如小米手环。
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class PeripheralManager(private val mActivity: FragmentActivity) : IBleManager {
    private val mExecutor: IExecutor by lazy { PeripheralExecutor(mActivity) }

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