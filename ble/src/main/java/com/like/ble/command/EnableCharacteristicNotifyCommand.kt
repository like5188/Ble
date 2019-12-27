package com.like.ble.command

import android.bluetooth.BluetoothAdapter
import java.util.*

/**
 * 开启特征通知命令
 *
 * @param address                       蓝牙设备地址
 * @param characteristicUuid            特征UUID
 * @param descriptorUuid                描述UUID，属于[characteristicUuid]
 * @param serviceUuid                   服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 * @param onSuccess                     命令执行成功回调
 * @param onFailure                     命令执行失败回调
 */
class EnableCharacteristicNotifyCommand(
    address: String,
    val characteristicUuid: UUID,
    val descriptorUuid: UUID = characteristicUuid,
    val serviceUuid: UUID? = null,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("设置通知特征值命令", address) {

    init {
        when {
            !BluetoothAdapter.checkBluetoothAddress(address) -> failureAndCompleteIfIncomplete("地址无效：$address")
        }
    }

    override suspend fun execute() {
        mReceiver?.enableCharacteristicNotify(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke()
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun getGroups(): Int = GROUP_CENTRAL or GROUP_CENTRAL_DEVICE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EnableCharacteristicNotifyCommand

        if (address != other.address) return false
        if (characteristicUuid != other.characteristicUuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        return result
    }

}