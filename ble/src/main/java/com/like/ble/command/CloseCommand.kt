package com.like.ble.command

/**
 * 关闭蓝牙命令
 */
class CloseCommand : Command("关闭蓝牙命令") {

    override suspend fun execute() {
        mReceiver?.close(this)
    }

    override fun getGroups(): Int = GROUP_CLOSE
}