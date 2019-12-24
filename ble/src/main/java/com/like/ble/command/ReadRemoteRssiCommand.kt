package com.like.ble.command

import android.bluetooth.BluetoothAdapter

/**
 * readRemoteRssi命令
 *
 * @param address                   蓝牙设备地址
 * @param timeout                   命令执行超时时间（毫秒）
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class ReadRemoteRssiCommand(
    address: String,
    val timeout: Long = 3000L,
    private val onSuccess: ((Int) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("readRemoteRssi命令", address) {

    init {
        when {
            !BluetoothAdapter.checkBluetoothAddress(address) -> failureAndCompleteIfIncomplete("地址无效：$address")
            timeout <= 0L -> failureAndCompleteIfIncomplete("timeout 必须大于 0")
        }
    }

    override suspend fun execute() {
        mReceiver?.readRemoteRssi(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            if (arg0 is Int) {
                onSuccess?.invoke(arg0)
            }
        }
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun getGroups(): Int = GROUP_CENTRAL or GROUP_CENTRAL_DEVICE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReadRemoteRssiCommand) return false

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

}