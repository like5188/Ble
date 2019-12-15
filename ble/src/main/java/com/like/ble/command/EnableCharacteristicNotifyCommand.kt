package com.like.ble.command

/**
 * 开启特征通知命令
 *
 * @param address                       蓝牙设备地址
 * @param characteristicUuidString      特征UUID
 * @param descriptorUuidString          描述UUID
 * @param onSuccess                     命令执行成功回调
 * @param onFailure                     命令执行失败回调
 */
class EnableCharacteristicNotifyCommand(
    val address: String,
    val characteristicUuidString: String,
    val descriptorUuidString: String = characteristicUuidString,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("设置通知特征值命令") {

    override fun execute() {
        mReceiver?.enableCharacteristicNotify(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke()
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EnableCharacteristicNotifyCommand

        if (address != other.address) return false
        if (characteristicUuidString != other.characteristicUuidString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + characteristicUuidString.hashCode()
        return result
    }

}