package com.like.ble.state

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.*
import com.like.ble.utils.findCharacteristic
import com.like.ble.utils.getBluetoothAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 蓝牙连接状态
 * 可以进行连接、操作数据等等操作
 */
class ConnectState : State() {
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mCommand: Command? = null
    private var mWriteJob: Job? = null
    private var mDelayJob: Job? = null

    private val mGattCallback = object : BluetoothGattCallback() {
        // 当连接状态改变
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {// 连接蓝牙设备成功
                    // 连接成功后，发现设备所有的 GATT Service
                    gatt.discoverServices()
                }
                BluetoothGatt.STATE_DISCONNECTED -> {// 连接蓝牙设备失败
                    mDelayJob?.cancel()
                    mBluetoothGatt = null
                    when (val command = mCommand) {
                        is DisconnectCommand -> {
                            command.successAndComplete()
                        }
                        else -> {
                            command?.failureAndComplete("连接蓝牙设备失败")
                        }
                    }
                }
            }
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            mDelayJob?.cancel()
            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                mBluetoothGatt = gatt
                when (val command = mCommand) {
                    is ConnectCommand -> {
                        command.successAndComplete()
                    }
                    is DisconnectCommand -> {
                        command.failureAndComplete("断开蓝牙连接失败")
                    }
                }
            } else {
                gatt.disconnect()
                mBluetoothGatt = null
                when (val command = mCommand) {
                    is DisconnectCommand -> {
                        command.successAndComplete()
                    }
                    else -> {
                        command?.failureAndComplete("连接蓝牙设备失败")
                    }
                }
            }
        }

        // 谁进行读数据操作，然后外围设备才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val command = mCommand
            if (command !is ReadCharacteristicCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.addDataToCache(characteristic.value)
                if (command.isWholeFrame()) {
                    mDelayJob?.cancel()
                    command.successAndComplete()
                }
            } else {
                mDelayJob?.cancel()
                command.failureAndComplete("读取特征值失败：${characteristic.uuid}")
            }
        }

        // 写特征值
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val command = mCommand
            if (command !is WriteCharacteristicCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (command.isAllWrite()) {
                    mDelayJob?.cancel()
                    command.successAndComplete()
                }
            } else {
                mWriteJob?.cancel()
                mDelayJob?.cancel()
                command.failureAndComplete("写特征值失败：${characteristic.uuid}")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val command = mCommand
            if (command !is SetMtuCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.successAndComplete(mtu)
            } else {
                command.failureAndComplete("failed to set mtu")
            }
        }

    }

    override fun connect(command: ConnectCommand) {
        if (mBluetoothGatt != null) {
            command.successAndComplete()
            return
        }

        if (command.address.isEmpty()) {
            command.failureAndComplete("连接蓝牙设备失败：地址不能为空")
            return
        }

        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            // 获取远端的蓝牙设备
            val bluetoothDevice = mActivity.getBluetoothAdapter()?.getRemoteDevice(command.address)
            if (bluetoothDevice == null) {
                command.failureAndComplete("连接蓝牙设备失败：设备 ${command.address} 未找到")
                return@launch
            }

            launch(Dispatchers.IO) {
                mCommand = command
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothDevice.connectGatt(mActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE)// 第二个参数表示是否自动重连
                } else {
                    bluetoothDevice.connectGatt(mActivity, false, mGattCallback)// 第二个参数表示是否自动重连
                }
            }

            mDelayJob = launch(Dispatchers.IO) {
                delay(command.connectTimeout)
                disconnect(DisconnectCommand(command.address))
                command.failureAndComplete("连接超时")
            }
        }
    }

    override fun disconnect(command: DisconnectCommand) {
        mCommand?.failureAndComplete("主动断开连接")
        mDelayJob?.cancel()

        if (mBluetoothGatt == null) {
            command.successAndComplete()
            return
        }
        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            mCommand = command
            mBluetoothGatt?.disconnect()
        }
    }

    override fun readCharacteristic(command: ReadCharacteristicCommand) {
        if (mBluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndComplete("特征值不存在：${command.characteristicUuidString}")
            return
        }

        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            mCommand = command
            mBluetoothGatt?.readCharacteristic(characteristic)
        }

        mDelayJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.readTimeout)
            command.failureAndComplete("读取特征值超时")
        }
    }

    override fun writeCharacteristic(command: WriteCharacteristicCommand) {
        if (mBluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        if (command.getBatchDataList().isEmpty()) {
            command.failureAndComplete("没有数据，无法写入")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndComplete("特征值不存在：${command.characteristicUuidString}")
            return
        }

        /*
        写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
        WRITE_TYPE_DEFAULT  默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
        WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
        WRITE_TYPE_SIGNED  写特征携带认证签名，具体作用不太清楚。
        */
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        mWriteJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            mCommand = command
            command.getBatchDataList().forEach {
                characteristic.value = it
                mBluetoothGatt?.writeCharacteristic(characteristic)
                delay(100)
            }
        }

        mDelayJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.writeTimeout)
            mWriteJob?.cancel()
            command.failureAndComplete("写特征值超时")
        }
    }

    override fun setMtu(command: SetMtuCommand) {
        if (mBluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                mCommand = command
                mBluetoothGatt?.requestMtu(command.mtu)
            }
        } else {
            command.failureAndComplete("android 5.0 才支持 setMtu() 操作")
        }
    }

    override fun close(command: CloseCommand) {
        mWriteJob?.cancel()
        mDelayJob?.cancel()
        mBluetoothGatt?.disconnect()
        mBluetoothGatt?.close()
        mBluetoothGatt = null
        mCommand?.failureAndComplete("主动断开连接")
        mCommand = null
        command.successAndComplete()
    }

}