package com.like.ble

import com.like.ble.command.base.Command
import com.like.ble.executor.IExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 蓝牙设备管理，使用者直接使用此接口进行相关操作。
 */
object BleManager {
    private var mExecutor: IExecutor? = null

    fun setExecutor(executor: IExecutor) {
        mExecutor = executor
    }

    suspend fun sendCommand(command: Command) {
        val executor = mExecutor
        if (executor == null) {
            command.failureAndCompleteIfIncomplete("mExecutor is null")
            return
        }
        withContext(Dispatchers.IO) {
            executor.execute(command)
        }
    }

    /**
     * 关闭所有资源
     */
    fun close() {
        mExecutor?.close()
    }

}