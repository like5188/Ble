package com.like.ble.command

import com.like.ble.utils.batch
import com.like.ble.utils.toByteArrayOrNull
import java.nio.ByteBuffer

/**
 * 写数据并等待获取数据命令（通过notify或者indicate的方式）
 *
 * @param address                   蓝牙设备地址
 * @param data                      需要写入的数据
 * @param characteristicUuidString  特征UUID
 * @param timeout                   命令执行超时时间（毫秒）
 * @param writeInterval             分包时，每次写入数据间隔超时时间（毫秒）
 * @param maxTransferSize           每次传输的最大字节数，用于分包，BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU（(最大为512字节）。
 * @param maxFrameTransferSize      每帧可以传输的最大字节数
 * @param isWholeFrame              是否是完整的一帧
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class WriteAndWaitForDataCommand(
    address: String,
    val data: ByteArray,
    val characteristicUuidString: String,
    val timeout: Long = 3000L,
    val writeInterval: Long = 200L,
    private val maxTransferSize: Int = 20,
    private val maxFrameTransferSize: Int = 1024,
    private val isWholeFrame: (ByteBuffer) -> Boolean = { true },
    private val onSuccess: ((ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("写数据并等待获取数据命令", address) {
    // 缓存读取特征数据时的返回数据，因为一帧有可能分为多次接收
    private val mDataCache: ByteBuffer by lazy { ByteBuffer.allocate(maxFrameTransferSize) }

    // 把数据分包
    private val mBatchDataList: List<ByteArray> by lazy { data.batch(maxTransferSize) }

    fun addDataToCache(data: ByteArray) {
        if (isCompleted()) return
        mDataCache.put(data)
    }

    fun isWholeFrame() = isWholeFrame(mDataCache)

    fun getBatchDataList() = mBatchDataList

    override suspend fun execute() {
        mReceiver?.writeAndWaitForData(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke(mDataCache.toByteArrayOrNull())
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun getGroups(): Int = GROUP_CENTRAL or GROUP_CENTRAL_DEVICE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WriteAndWaitForDataCommand) return false

        if (!data.contentEquals(other.data)) return false
        if (address != other.address) return false
        if (characteristicUuidString != other.characteristicUuidString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + characteristicUuidString.hashCode()
        return result
    }

}