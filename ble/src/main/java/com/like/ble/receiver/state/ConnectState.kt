package com.like.ble.receiver.state

import android.bluetooth.*
import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.*
import com.like.ble.model.BleResult
import com.like.ble.model.BleStatus
import com.like.ble.receiver.StateAdapter
import com.like.ble.utils.batch
import com.like.ble.utils.findCharacteristic
import com.like.ble.utils.getBluetoothAdapter
import com.like.ble.utils.toByteArrayOrNull
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.nio.ByteBuffer
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * 蓝牙连接状态
 * 可以进行连接、操作数据等等操作
 */
class ConnectState(
    private val mActivity: FragmentActivity,
    private val mLiveData: MutableLiveData<BleResult>
) : StateAdapter() {
    private var delayJob: Job? = null
    private val mChannels: MutableMap<String, Channel<ICommand>> = mutableMapOf()
    private val mConnectedBluetoothGattList = mutableListOf<BluetoothGatt>()
    // 连接蓝牙设备的回调函数
    private val mGattCallback = object : BluetoothGattCallback() {
        // 当连接状态改变
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {// 连接蓝牙设备成功
                    // 连接成功后，发现设备所有的 GATT Service
                    gatt.discoverServices()
                }
                BluetoothGatt.STATE_DISCONNECTED -> {// 连接蓝牙设备失败
                    closeChannelAndRemove(gatt.device.address)
                    closeGattAndRemove(gatt)
                    mLiveData.postValue(BleResult(BleStatus.DISCONNECTED, gatt.device.address))
                }
            }
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val address = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                addChannelAndReceive(address, gatt)
                mConnectedBluetoothGattList.add(gatt)
                mLiveData.postValue(BleResult(BleStatus.CONNECTED, gatt.device.address))
            } else {
                closeChannelAndRemove(address)
                closeGattAndRemove(gatt)
                mLiveData.postValue(BleResult(BleStatus.DISCONNECTED, gatt.device.address))
            }
        }

        // 外围设备调用 notifyCharacteristicChanged() 通知所有中心设备，数据改变了，此方法被触发。
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            mLiveData.postValue(BleResult(BleStatus.ON_CHARACTERISTIC_CHANGED, characteristic.value))
        }

        // 谁进行读数据操作，然后外围设备才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mLiveData.postValue(BleResult(BleStatus.ON_CHARACTERISTIC_READ_SUCCESS, characteristic.value))
            } else {
                mLiveData.postValue(BleResult(BleStatus.ON_CHARACTERISTIC_READ_FAILURE, characteristic.value))
            }
        }

        // 写特征值
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mLiveData.postValue(BleResult(BleStatus.ON_CHARACTERISTIC_WRITE_SUCCESS, characteristic.value))
            } else {
                mLiveData.postValue(BleResult(BleStatus.ON_CHARACTERISTIC_WRITE_FAILURE, characteristic.value))
            }
        }

        // 读描述值
        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mLiveData.postValue(BleResult(BleStatus.ON_DESCRIPTOR_READ_SUCCESS, descriptor.value))
            } else {
                mLiveData.postValue(BleResult(BleStatus.ON_DESCRIPTOR_READ_FAILURE, descriptor.value))
            }
        }

        // 写描述值
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mLiveData.postValue(BleResult(BleStatus.ON_DESCRIPTOR_WRITE_SUCCESS, descriptor.value))
            } else {
                mLiveData.postValue(BleResult(BleStatus.ON_DESCRIPTOR_WRITE_FAILURE, descriptor.value))
            }
        }

        // 读蓝牙信号值
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mLiveData.postValue(BleResult(BleStatus.ON_READ_REMOTE_RSSI_SUCCESS, rssi))
            } else {
                mLiveData.postValue(BleResult(BleStatus.ON_READ_REMOTE_RSSI_FAILURE, rssi))
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mLiveData.postValue(BleResult(BleStatus.ON_MTU_CHANGED_SUCCESS, mtu))
            } else {
                mLiveData.postValue(BleResult(BleStatus.ON_MTU_CHANGED_FAILURE, mtu))
            }
        }

    }

    override fun connect(command: ConnectCommand) {
        super.connect(command)
        if (isConnected(command.address)) return

        if (command.address.isEmpty()) {
            command.onFailure?.invoke(IllegalArgumentException("连接蓝牙设备失败：地址不能为空"))
            return
        }

        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            // 获取远端的蓝牙设备
            val bluetoothDevice = mActivity.getBluetoothAdapter()?.getRemoteDevice(command.address)
            if (bluetoothDevice == null) {
                command.onFailure?.invoke(IllegalArgumentException("连接蓝牙设备失败：设备 ${command.address} 未找到"))
                return@launch
            }

            // 在任何时刻都只能最多一个设备在尝试建立连接。如果同时对多个蓝牙设备发起建立 Gatt 连接请求。如果前面的设备连接失败了，后面的设备请求会被永远阻塞住，不会有任何连接回调。
            // 对BLE设备连接，连接过程要尽量短，如果连接不上，不要盲目进行重连，否这你的电池会很快被消耗掉。
            var observer: Observer<BleResult>? = null
            observer = Observer { bleResult ->
                if (bleResult?.status == BleStatus.CONNECTED) {
                    delayJob?.cancel()
                    command.onSuccess?.invoke()
                } else if (bleResult?.status == BleStatus.DISCONNECTED) {
                    delayJob?.cancel()
                    removeObserver(observer)
                    command.onFailure?.invoke(RuntimeException("连接蓝牙设备失败"))
                }
            }

            withContext(Dispatchers.Main) {
                mLiveData.value = null// 避免残留值影响下次命令
                mLiveData.observe(mActivity, observer)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothDevice.connectGatt(mActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE)// 第二个参数表示是否自动重连
            } else {
                bluetoothDevice.connectGatt(mActivity, false, mGattCallback)// 第二个参数表示是否自动重连
            }

            delayJob = launch((Dispatchers.IO)) {
                delay(command.connectTimeout)
                disconnect(DisconnectCommand(command.address))
            }
        }
    }

    override fun disconnect(command: DisconnectCommand) {
        super.disconnect(command)
        if (!isConnected(command.address)) return
        val listIterator = mConnectedBluetoothGattList.listIterator()
        while (listIterator.hasNext()) {
            val gatt = listIterator.next()
            if (gatt.device.address == command.address) {

                mActivity.lifecycleScope.launch(Dispatchers.IO) {
                    var observer: Observer<BleResult>? = null
                    observer = Observer { bleResult ->
                        if (bleResult?.status == BleStatus.CONNECTED) {
                            command.onFailure?.invoke(RuntimeException("断开蓝牙连接失败"))
                        } else if (bleResult?.status == BleStatus.DISCONNECTED) {
                            removeObserver(observer)
                            command.onSuccess?.invoke()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        mLiveData.value = null// 避免残留值影响下次命令
                        mLiveData.observe(mActivity, observer)
                    }

                    gatt.disconnect()
                }

                listIterator.remove()
                return
            }
        }
        mLiveData.postValue(BleResult(BleStatus.DISCONNECTED, command.address))
    }

    override fun read(command: ReadCommand) {
        super.read(command)
        if (!isConnected(command.address)) return
        if (!mChannels.containsKey(command.address)) {
            mLiveData.postValue(BleResult(BleStatus.ON_CHARACTERISTIC_READ_FAILURE, errorMsg = "设备未连接 $command"))
            return
        }

        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            mChannels[command.address]?.send(command)
        }
    }

    override fun write(command: WriteCommand) {
        super.write(command)
        if (!isConnected(command.address)) return
        if (!mChannels.containsKey(command.address)) {
            mLiveData.postValue(BleResult(BleStatus.ON_CHARACTERISTIC_WRITE_FAILURE, errorMsg = "设备未连接 $command"))
            return
        }

        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            mChannels[command.address]?.send(command)
        }
    }

    override fun setMtu(command: SetMtuCommand) {
        super.setMtu(command)
        if (!isConnected(command.address)) return
        if (!mChannels.containsKey(command.address)) {
            mLiveData.postValue(BleResult(BleStatus.ON_MTU_CHANGED_FAILURE, errorMsg = "设备未连接 $command"))
            return
        }

        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            mChannels[command.address]?.send(command)
        }
    }

    override fun close(command: CloseCommand) {
        super.close(command)
        mChannels.values.forEach {
            it.close()
        }
        mChannels.clear()

        mConnectedBluetoothGattList.forEach {
            it.disconnect()
            it.close()
        }
        mConnectedBluetoothGattList.clear()
    }

    private fun isConnected(address: String): Boolean = mConnectedBluetoothGattList.any { it.device.address == address }

    private fun removeObserver(observer: Observer<BleResult>?) {
        observer ?: return
        mActivity.runOnUiThread {
            mLiveData.removeObserver(observer)
        }
    }

    /**
     * 添加指定 address 的通道，并开启接收数据
     */
    private fun addChannelAndReceive(address: String, gatt: BluetoothGatt) {
        if (address.isEmpty()) return
        if (!mChannels.containsKey(address)) {
            val channel = Channel<ICommand>()
            mChannels[address] = channel
            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                for (command in channel) {
                    when (command) {
                        is ReadCommand -> {
                            // 缓存返回数据，因为一帧有可能分为多次接收
                            val resultCache: ByteBuffer = ByteBuffer.allocate(command.maxFrameTransferSize)
                            // 过期时间
                            val expired = command.readTimeout + System.currentTimeMillis()

                            // 是否过期
                            fun isExpired() = expired - System.currentTimeMillis() <= 0

                            /**
                             * 此条命令是否已经完成。成功或者失败
                             */
                            var isCompleted = false

                            var readObserver: Observer<BleResult>? = null
                            readObserver = Observer { bleResult ->
                                if (bleResult?.status == BleStatus.ON_CHARACTERISTIC_READ_SUCCESS) {
                                    if (isCompleted) {// 说明超时了，避免超时后继续返回数据（此时没有发送下一条数据）
                                        return@Observer
                                    }
                                    resultCache.put(bleResult.data as ByteArray)
                                    if (command.isWholeFrame(resultCache)) {
                                        isCompleted = true
                                        removeObserver(readObserver)
                                        command.onSuccess?.invoke(resultCache.toByteArrayOrNull())
                                    }
                                } else if (bleResult?.status == BleStatus.ON_CHARACTERISTIC_READ_FAILURE) {
                                    isCompleted = true
                                    removeObserver(readObserver)
                                    command.onFailure?.invoke(RuntimeException("读取特征值失败：${command.characteristicUuidString}"))
                                }
                            }

                            if (isCompleted) {
                                command.onFailure?.invoke(IllegalArgumentException("此命令已经完成"))
                                return@launch
                            }

                            val characteristic = gatt.findCharacteristic(command.characteristicUuidString)
                            if (characteristic == null) {
                                command.onFailure?.invoke(IllegalArgumentException("特征值不存在：${command.characteristicUuidString}"))
                                return@launch
                            }

                            mActivity.lifecycleScope.launch(Dispatchers.Main) {
                                mLiveData.value = null// 避免残留值影响下次命令
                                mLiveData.observe(mActivity, readObserver)

                                launch(Dispatchers.IO) {
                                    gatt.readCharacteristic(characteristic)
                                }

                                withContext(Dispatchers.IO) {
                                    while (!isCompleted) {
                                        delay(100)
                                        if (isExpired()) {// 说明是超时了
                                            isCompleted = true
                                            removeObserver(readObserver)
                                            command.onFailure?.invoke(TimeoutException())
                                            return@withContext
                                        }
                                    }
                                }
                            }
                        }
                        is WriteCommand -> {
                            val mDataList: List<ByteArray> by lazy { command.data.batch(command.maxTransferSize) }
                            // 记录所有的数据批次，在所有的数据都发送完成后，才调用onSuccess()
                            val mBatchCount: AtomicInteger by lazy { AtomicInteger(mDataList.size) }
                            // 过期时间
                            val expired = command.writeTimeout + System.currentTimeMillis()

                            // 是否过期
                            fun isExpired(): Boolean = expired - System.currentTimeMillis() <= 0

                            if (mBatchCount.get() == 0) {
                                command.onFailure?.invoke(IllegalArgumentException("此命令已经完成"))
                                return@launch
                            }

                            val characteristic = gatt.findCharacteristic(command.characteristicUuidString)
                            if (characteristic == null) {
                                command.onFailure?.invoke(IllegalArgumentException("特征值不存在：${command.characteristicUuidString}"))
                                return@launch
                            }
                            /*
                            写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
                                WRITE_TYPE_DEFAULT  默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
                                WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
                                WRITE_TYPE_SIGNED  写特征携带认证签名，具体作用不太清楚。
                             */
                            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

                            var job: Job? = null
                            var writeObserver: Observer<BleResult>? = null
                            writeObserver = Observer { bleResult ->
                                if (bleResult?.status == BleStatus.ON_CHARACTERISTIC_WRITE_SUCCESS) {
                                    if (mBatchCount.decrementAndGet() <= 0) {
                                        removeObserver(writeObserver)
                                        command.onSuccess?.invoke()
                                    }
                                } else if (bleResult?.status == BleStatus.ON_CHARACTERISTIC_WRITE_FAILURE) {
                                    job?.cancel()
                                    removeObserver(writeObserver)
                                    mBatchCount.set(0)
                                    command.onFailure?.invoke(RuntimeException("写特征值失败：${command.characteristicUuidString}"))
                                }
                            }

                            mActivity.lifecycleScope.launch(Dispatchers.Main) {
                                mLiveData.value = null// 避免残留值影响下次命令
                                mLiveData.observe(mActivity, writeObserver)

                                job = launch(Dispatchers.IO) {
                                    mDataList.forEach {
                                        characteristic.value = it
                                        gatt.writeCharacteristic(characteristic)
                                        delay(1000)
                                    }
                                }

                                withContext(Dispatchers.IO) {
                                    while (mBatchCount.get() > 0) {
                                        delay(100)
                                        if (isExpired()) {// 说明是超时了
                                            job?.cancel()
                                            removeObserver(writeObserver)
                                            command.onFailure?.invoke(TimeoutException())
                                            return@withContext
                                        }
                                    }
                                }
                            }
                        }
                        is SetMtuCommand -> {
                            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                                var setMtuObserver: Observer<BleResult>? = null
                                setMtuObserver = Observer { bleResult ->
                                    if (bleResult?.status == BleStatus.ON_MTU_CHANGED_SUCCESS) {
                                        removeObserver(setMtuObserver)
                                        command.onSuccess?.invoke(command.mtu)
                                    } else if (bleResult?.status == BleStatus.ON_MTU_CHANGED_FAILURE) {
                                        removeObserver(setMtuObserver)
                                        command.onFailure?.invoke(RuntimeException("设置MTU失败"))
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    mLiveData.value = null// 避免残留值影响下次命令
                                    mLiveData.observe(mActivity, setMtuObserver)
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    gatt.requestMtu(command.mtu)
                                } else {
                                    command.onFailure?.invoke(RuntimeException("android 5.0 才支持 setMtu() 操作"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 关闭指定 address 的通道，并移除
     */
    private fun closeChannelAndRemove(address: String) {
        if (address.isEmpty()) return
        if (mChannels.containsKey(address)) {
            mChannels[address]?.close()
            mChannels.remove(address)
        }
    }

    /**
     * 关闭指定 BluetoothGatt，并移除
     */
    private fun closeGattAndRemove(gatt: BluetoothGatt) {
        gatt.close()
        mConnectedBluetoothGattList.remove(gatt)
    }
}