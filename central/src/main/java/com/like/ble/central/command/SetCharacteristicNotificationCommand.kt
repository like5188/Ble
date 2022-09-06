package com.like.ble.central.command

import com.like.ble.central.command.SetCharacteristicNotificationCommand.Companion.TYPE_INDICATE
import com.like.ble.central.command.SetCharacteristicNotificationCommand.Companion.TYPE_NOTIFY
import java.util.*

/**
 * 设置特征的notification或者indication的命令
 * 配合[WriteDescriptorCommand]，组合成[MacroCommand]，来使能notification或者indication。
 *
 * @param characteristicUuid            特征UUID
 * @param descriptorUuid                描述UUID，属于[characteristicUuid]
 * @param serviceUuid                   服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 * @param type                          类型：[TYPE_NOTIFY]、[TYPE_INDICATE]
 * @param enable                        true：开启；false：关闭
 */
class SetCharacteristicNotificationCommand(
    address: String,
    val characteristicUuid: UUID,
    val descriptorUuid: UUID = characteristicUuid,
    val serviceUuid: UUID? = null,
    val type: Int = TYPE_NOTIFY,
    val enable: Boolean = true,
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null
) : AddressCommand("设置特征的notification或者indication的命令", onCompleted = onCompleted, onError = onError, address = address) {
    companion object {
        const val TYPE_NOTIFY = 0
        const val TYPE_INDICATE = 1
    }

    init {
        if (type != TYPE_NOTIFY && type != TYPE_INDICATE) {
            errorAndComplete("type can only be 0 or 1")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SetCharacteristicNotificationCommand) return false
        if (!super.equals(other)) return false

        if (characteristicUuid != other.characteristicUuid) return false
        if (descriptorUuid != other.descriptorUuid) return false
        if (serviceUuid != other.serviceUuid) return false
        if (type != other.type) return false
        if (enable != other.enable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        result = 31 * result + descriptorUuid.hashCode()
        result = 31 * result + (serviceUuid?.hashCode() ?: 0)
        result = 31 * result + type
        result = 31 * result + enable.hashCode()
        return result
    }

}