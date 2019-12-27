package com.like.ble.command

import android.bluetooth.BluetoothAdapter
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 写描述值命令
 *
 * @param address                   蓝牙设备地址
 * @param data                      需要写入的数据，已经分好包了的，每次传递一个 ByteArray。BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU（(最大为512字节）。
 * @param descriptorUuid            描述UUID，属于[characteristicUuid]
 * @param characteristicUuid        特征UUID，如果不为null，则会在此特征下查找[descriptorUuid]；如果为null，则会遍历所有特征查找第一个匹配的[descriptorUuid]
 * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 * @param timeout                   命令执行超时时间（毫秒）
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class WriteDescriptorCommand(
    address: String,
    val data: List<ByteArray>,
    val descriptorUuid: UUID,
    val characteristicUuid: UUID? = null,
    val serviceUuid: UUID? = null,
    val timeout: Long = 3000L,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("写描述值命令", address) {

    init {
        when {
            !BluetoothAdapter.checkBluetoothAddress(address) -> failureAndCompleteIfIncomplete("地址无效：$address")
            data.isEmpty() -> failureAndCompleteIfIncomplete("data 不能为空")
            timeout <= 0L -> failureAndCompleteIfIncomplete("timeout 必须大于 0")
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
        mReceiver?.writeDescriptor(this)
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

        other as WriteDescriptorCommand

        if (data != other.data) return false
        if (address != other.address) return false
        if (descriptorUuid != other.descriptorUuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + descriptorUuid.hashCode()
        return result
    }

}