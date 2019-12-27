package com.like.ble.command

import android.bluetooth.BluetoothAdapter
import java.util.*

/**
 * 读描述值命令，一次最多可以读取600字节
 *
 * @param address                   蓝牙设备地址
 * @param descriptorUuid            描述UUID，属于[characteristicUuid]
 * @param characteristicUuid        特征UUID，如果不为null，则会在此特征下查找[descriptorUuid]；如果为null，则会遍历所有特征查找第一个匹配的[descriptorUuid]
 * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 * @param timeout                   命令执行超时时间（毫秒）
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class ReadDescriptorCommand(
    address: String,
    val descriptorUuid: UUID,
    val characteristicUuid: UUID? = null,
    val serviceUuid: UUID? = null,
    val timeout: Long = 3000L,
    private val onSuccess: ((ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("读描述值命令", address) {

    init {
        when {
            !BluetoothAdapter.checkBluetoothAddress(address) -> failureAndCompleteIfIncomplete("地址无效：$address")
            timeout <= 0L -> failureAndCompleteIfIncomplete("timeout 必须大于 0")
        }
    }

    override suspend fun execute() {
        mReceiver?.readDescriptor(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            if (arg0 is ByteArray?) {
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
        if (other !is ReadDescriptorCommand) return false

        if (address != other.address) return false
        if (descriptorUuid != other.descriptorUuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + descriptorUuid.hashCode()
        return result
    }

}