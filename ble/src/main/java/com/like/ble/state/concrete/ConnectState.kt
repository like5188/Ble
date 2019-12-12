package com.like.ble.state.concrete

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.Command
import com.like.ble.command.concrete.*
import com.like.ble.state.StateAdapter
import com.like.ble.utils.batch
import com.like.ble.utils.findCharacteristic
import com.like.ble.utils.getBluetoothAdapter
import com.like.ble.utils.toByteArrayOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 蓝牙连接状态
 * 可以进行连接、操作数据等等操作
 */
class ConnectState : StateAdapter() {
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mCommand: Command? = null
    private var mJob: Job? = null

    // 记录写入所有的数据批次，在所有的数据都发送完成后，才调用onSuccess()
    private val mWriteCharacteristicBatchCount: AtomicInteger = AtomicInteger(0)

    // 读取特征数据是否完成
    private val mIsReadCharacteristicCompleted = AtomicBoolean(false)
    // 缓存读取特征数据时的返回数据，因为一帧有可能分为多次接收
    private var mReadCharacteristicDataCache: ByteBuffer? = null

    private val mGattCallback = object : BluetoothGattCallback() {
        // 当连接状态改变
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {// 连接蓝牙设备成功
                    // 连接成功后，发现设备所有的 GATT Service
                    gatt.discoverServices()
                }
                BluetoothGatt.STATE_DISCONNECTED -> {// 连接蓝牙设备失败
                    mBluetoothGatt = null
                    mJob?.cancel()
                    when (val command = mCommand) {
                        is ConnectCommand -> {
                            command.onFailure?.invoke(RuntimeException("连接蓝牙设备失败"))
                        }
                        is DisconnectCommand -> {
                            command.onSuccess?.invoke()
                        }
                    }
                }
            }
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                mBluetoothGatt = gatt
                mJob?.cancel()
                when (val command = mCommand) {
                    is ConnectCommand -> {
                        command.onSuccess?.invoke()
                    }
                    is DisconnectCommand -> {
                        command.onFailure?.invoke(RuntimeException("断开蓝牙连接失败"))
                    }
                }
            } else {
                gatt.disconnect()
                mBluetoothGatt = null
                mJob?.cancel()
                when (val command = mCommand) {
                    is ConnectCommand -> {
                        command.onFailure?.invoke(RuntimeException("连接蓝牙设备失败"))
                    }
                    is DisconnectCommand -> {
                        command.onSuccess?.invoke()
                    }
                }
            }
        }

        // 谁进行读数据操作，然后外围设备才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val command = mCommand
            if (command !is ReadCharacteristicCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mIsReadCharacteristicCompleted.get()) {// 说明超时了，避免超时后继续返回数据（此时没有发送下一条数据）
                    return
                }
                mReadCharacteristicDataCache?.let {
                    it.put(characteristic.value)
                    if (command.isWholeFrame(it)) {
                        mIsReadCharacteristicCompleted.set(true)
                        command.onSuccess?.invoke(it.toByteArrayOrNull())
                    }
                }
            } else {
                mIsReadCharacteristicCompleted.set(true)
                command.onFailure?.invoke(RuntimeException("读取特征值失败：$characteristic"))
            }
        }

        // 写特征值
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val command = mCommand
            if (command !is WriteCharacteristicCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mWriteCharacteristicBatchCount.decrementAndGet() <= 0) {
                    command.onSuccess?.invoke()
                }
            } else {
                mJob?.cancel()
                mWriteCharacteristicBatchCount.set(0)
                command.onFailure?.invoke(RuntimeException("写特征值失败：$characteristic"))
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val command = mCommand
            if (command !is SetMtuCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.onSuccess?.invoke(mtu)
            } else {
                command.onFailure?.invoke(RuntimeException("failed to set mtu"))
            }
        }

    }

    override fun connect(command: ConnectCommand) {
        super.connect(command)
        mCommand = command

        if (mBluetoothGatt != null) {
            command.onSuccess?.invoke()
            return
        }

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

            launch(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothDevice.connectGatt(mActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE)// 第二个参数表示是否自动重连
                } else {
                    bluetoothDevice.connectGatt(mActivity, false, mGattCallback)// 第二个参数表示是否自动重连
                }
            }

            mJob = launch(Dispatchers.IO) {
                delay(command.connectTimeout)
                disconnect(DisconnectCommand(command.address))
            }
        }
    }

    override fun disconnect(command: DisconnectCommand) {
        super.disconnect(command)
        mCommand = command

        if (mBluetoothGatt == null) {
            command.onSuccess?.invoke()
            return
        }
        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            mBluetoothGatt?.disconnect()
        }
    }

    override fun readCharacteristic(command: ReadCharacteristicCommand) {
        super.readCharacteristic(command)
        mCommand = command

        if (mBluetoothGatt == null) {
            command.onFailure?.invoke(IllegalArgumentException("设备未连接：${command.address}"))
            return
        }

        // 过期时间
        val expired = command.readTimeout + System.currentTimeMillis()

        // 是否过期
        fun isExpired() = expired - System.currentTimeMillis() <= 0

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.onFailure?.invoke(IllegalArgumentException("特征值不存在：${command.characteristicUuidString}"))
            return
        }

        mIsReadCharacteristicCompleted.set(false)
        mReadCharacteristicDataCache = ByteBuffer.allocate(command.maxFrameTransferSize)

        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            mBluetoothGatt?.readCharacteristic(characteristic)
        }

        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            while (!mIsReadCharacteristicCompleted.get()) {
                delay(100)
                if (isExpired()) {// 说明是超时了
                    mIsReadCharacteristicCompleted.set(true)
                    command.onFailure?.invoke(TimeoutException())
                    return@launch
                }
            }
        }
    }

    override fun writeCharacteristic(command: WriteCharacteristicCommand) {
        super.writeCharacteristic(command)
        mCommand = command

        if (mBluetoothGatt == null) {
            command.onFailure?.invoke(IllegalArgumentException("设备未连接：${command.address}"))
            return
        }

        val dataList: List<ByteArray> by lazy { command.data.batch(command.maxTransferSize) }
        if (dataList.isEmpty()) {
            command.onFailure?.invoke(IllegalArgumentException("没有数据，无法写入"))
            return
        }

        // 过期时间
        val expired = command.writeTimeout + System.currentTimeMillis()

        // 是否过期
        fun isExpired(): Boolean = expired - System.currentTimeMillis() <= 0

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.onFailure?.invoke(IllegalArgumentException("特征值不存在：${command.characteristicUuidString}"))
            return
        }

        // 记录所有的数据批次，在所有的数据都发送完成后，才调用onSuccess()
        mWriteCharacteristicBatchCount.set(dataList.size)

        /*
        写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
        WRITE_TYPE_DEFAULT  默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
        WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
        WRITE_TYPE_SIGNED  写特征携带认证签名，具体作用不太清楚。
        */
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        mJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            dataList.forEach {
                characteristic.value = it
                mBluetoothGatt?.writeCharacteristic(characteristic)
                delay(1000)
            }
        }

        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            while (mWriteCharacteristicBatchCount.get() > 0) {
                delay(100)
                if (isExpired()) {// 说明是超时了
                    mJob?.cancel()
                    command.onFailure?.invoke(TimeoutException())
                    return@launch
                }
            }
        }
    }

    override fun setMtu(command: SetMtuCommand) {
        super.setMtu(command)
        mCommand = command

        if (mBluetoothGatt == null) {
            command.onFailure?.invoke(IllegalArgumentException("设备未连接：${command.address}"))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                mBluetoothGatt?.requestMtu(command.mtu)
            }
        } else {
            command.onFailure?.invoke(RuntimeException("android 5.0 才支持 setMtu() 操作"))
        }

    }

    override fun close(command: CloseCommand) {
        super.close(command)
        mBluetoothGatt?.disconnect()
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

}