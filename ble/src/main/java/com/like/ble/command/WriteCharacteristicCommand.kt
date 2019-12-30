package com.like.ble.command

import com.like.ble.command.base.AddressCommand
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 写特征值命令
 *
 * @param data                      需要写入的数据，已经分好包了的，每次传递一个 ByteArray。BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU（(最大为512字节）。
 * @param characteristicUuid        特征UUID
 * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class WriteCharacteristicCommand(
    address: String,
    val data: List<ByteArray>,
    val characteristicUuid: UUID,
    val serviceUuid: UUID? = null,
    timeout: Long = 3000L,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : AddressCommand("写特征值命令", timeout, address) {

    init {
        if (data.isEmpty()) {
            failureAndCompleteIfIncomplete("data cannot be empty")
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

    override suspend fun execute() {
        mReceiver?.writeCharacteristic(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke()
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WriteCharacteristicCommand) return false
        if (!super.equals(other)) return false

        if (characteristicUuid != other.characteristicUuid) return false
        if (serviceUuid != other.serviceUuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        result = 31 * result + (serviceUuid?.hashCode() ?: 0)
        return result
    }

}