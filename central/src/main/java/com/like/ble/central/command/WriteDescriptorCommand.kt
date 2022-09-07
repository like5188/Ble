package com.like.ble.central.command

import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 写描述值命令
 *
 * @param data                      需要写入的数据，已经分好包了的，每次传递一个 ByteArray。BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU（(最大为512字节）。
 * @param descriptorUuid            描述UUID，属于[characteristicUuid]
 * @param characteristicUuid        特征UUID，如果不为null，则会在此特征下查找[descriptorUuid]；如果为null，则会遍历所有特征查找第一个匹配的[descriptorUuid]
 * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 */
class WriteDescriptorCommand(
    address: String,
    val data: List<ByteArray>,
    val descriptorUuid: UUID,
    val characteristicUuid: UUID? = null,
    val serviceUuid: UUID? = null,
    timeout: Long = 3000L,
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null
) : AddressCommand("写描述值命令", timeout = timeout, onCompleted = onCompleted, onError = onError, address = address) {

    init {
        if (data.isEmpty()) {
            error("data cannot be empty")
        }
    }

    // 记录写入所有的数据批次，在所有的数据都发送完成后，才调用onSuccess()
    private val mWriteCount: AtomicInteger by lazy { AtomicInteger(data.size) }

    // 是否可以进行下一批次的写入操作
    private val mNextFlag = AtomicBoolean(false)

    fun isAllWrite(): Boolean {
        mNextFlag.set(true)
        return mWriteCount.decrementAndGet() <= 0
    }

    internal suspend fun waitForNextFlag() {
        while (!mNextFlag.get()) {
            delay(20)
        }
        mNextFlag.set(false)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WriteDescriptorCommand) return false
        if (!super.equals(other)) return false

        if (descriptorUuid != other.descriptorUuid) return false
        if (characteristicUuid != other.characteristicUuid) return false
        if (serviceUuid != other.serviceUuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + descriptorUuid.hashCode()
        result = 31 * result + (characteristicUuid?.hashCode() ?: 0)
        result = 31 * result + (serviceUuid?.hashCode() ?: 0)
        return result
    }

}