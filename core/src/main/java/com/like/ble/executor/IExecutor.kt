package com.like.ble.executor

/**
 * 蓝牙命令真正的执行者。
 */
interface IExecutor {
    /**
     * 释放资源
     */
    suspend fun close()

}
