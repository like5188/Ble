package com.like.ble.command

/**
 * 读特征值命令，一次最多可以读取600字节
 *
 * @param address                   蓝牙设备地址
 * @param characteristicUuidString  特征UUID
 * @param timeout                   命令执行超时时间（毫秒）
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class ReadCharacteristicCommand(
    address: String,
    val characteristicUuidString: String,
    val timeout: Long = 3000L,
    private val onSuccess: ((ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("读特征值命令", address) {

    override suspend fun execute() {
        mReceiver?.readCharacteristic(this)
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
        if (other !is ReadCharacteristicCommand) return false

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