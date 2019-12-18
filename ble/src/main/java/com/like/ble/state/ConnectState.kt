package com.like.ble.state

import android.bluetooth.*
import android.os.Build
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.*
import com.like.ble.utils.findCharacteristic
import com.like.ble.utils.getBluetoothAdapter
import com.like.ble.utils.getUuidValidString
import com.like.ble.utils.getValidString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 蓝牙连接状态
 * 可以进行连接、操作数据等等操作
 */
class ConnectState : State() {
    private var mBluetoothGatt: BluetoothGatt? = null
    // 蓝牙Gatt回调方法中都不可以进行耗时操作，需要将其方法内进行的操作丢进另一个线程，尽快返回。
    private val mGattCallback = object : BluetoothGattCallback() {
        // 当连接状态改变
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothGatt.STATE_CONNECTED -> {// 连接蓝牙设备成功
                        // 连接成功后，发现设备所有的 GATT Service
                        gatt.discoverServices()
                    }
                    BluetoothGatt.STATE_DISCONNECTED -> {// 连接蓝牙设备失败
                        mBluetoothGatt = null
                        when (val curCommand = mCurCommand) {
                            is DisconnectCommand -> {
                                curCommand.successAndComplete()
                            }
                            else -> {
                                curCommand?.failureAndComplete("连接蓝牙设备失败")
                            }
                        }
                    }
                }
            } else {
                mBluetoothGatt = null
                when (val curCommand = mCurCommand) {
                    is DisconnectCommand -> {
                        curCommand.failureAndComplete("断开连接失败")
                    }
                    else -> {
                        curCommand?.failureAndComplete("连接蓝牙设备失败")
                    }
                }
            }
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val curCommand = mCurCommand
            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                mBluetoothGatt = gatt
                when (curCommand) {
                    is ConnectCommand -> {
                        curCommand.successAndComplete()
                    }
                    is DisconnectCommand -> {
                        curCommand.failureAndComplete("断开蓝牙连接失败")
                    }
                    else -> {
                        curCommand?.failureAndComplete("蓝牙设备重新连接成功")
                    }
                }
            } else {
                gatt.disconnect()
                mBluetoothGatt = null
                when (curCommand) {
                    is DisconnectCommand -> {
                        curCommand.successAndComplete()
                    }
                    else -> {
                        curCommand?.failureAndComplete("连接蓝牙设备失败，发现服务失败")
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val curCommand = mCurCommand
            if (curCommand !is WriteAndWaitForDataCommand) return
            curCommand.addDataToCache(characteristic.value)
            if (curCommand.isWholeFrame()) {
                curCommand.successAndComplete()
            }
        }

        // 谁进行读数据操作，然后外围设备才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.e("ConnectState", "onCharacteristicRead value=${Arrays.toString(characteristic.value)}")
            val curCommand = mCurCommand
            if (curCommand !is ReadCharacteristicCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                curCommand.addDataToCache(characteristic.value)
                if (curCommand.isWholeFrame()) {
                    curCommand.successAndComplete()
                }
            } else {
                curCommand.failureAndComplete("读取特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        // 写特征值，注意，这里的characteristic.value中的数据是你写入的数据，而不是外围设备sendResponse返回的。
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.e("ConnectState", "onCharacteristicRead value=${Arrays.toString(characteristic.value)}")
            val curCommand = mCurCommand
            if (curCommand !is WriteCharacteristicCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (curCommand.isAllWrite()) {
                    curCommand.successAndComplete()
                }
            } else {
                curCommand.failureAndComplete("写特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            val curCommand = mCurCommand
            if (curCommand !is EnableCharacteristicNotifyCommand &&
                curCommand !is DisableCharacteristicNotifyCommand &&
                curCommand !is EnableCharacteristicIndicateCommand &&
                curCommand !is DisableCharacteristicIndicateCommand
            ) {
                return
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                curCommand.successAndComplete()
            } else {
                curCommand.failureAndComplete("writeDescriptor fail")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val curCommand = mCurCommand
            if (curCommand !is SetMtuCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                curCommand.successAndComplete(mtu)
            } else {
                curCommand.failureAndComplete("failed to set mtu")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            val curCommand = mCurCommand
            if (curCommand !is ReadRemoteRssiCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                curCommand.successAndComplete(rssi)
            } else {
                curCommand.failureAndComplete("failed to read remote rssi")
            }
        }

    }

    @Synchronized
    override fun connect(command: ConnectCommand) {
        mCurCommand = command// todo 连接命令要一直保存。这样在断开时才会触发回调更新界面

        if (mBluetoothGatt != null) {
            command.successAndComplete()
            return
        }

        // 获取远端的蓝牙设备
        val bluetoothDevice = mActivity.getBluetoothAdapter()?.getRemoteDevice(command.address)
        if (bluetoothDevice == null) {
            command.failureAndComplete("连接蓝牙设备失败：设备 ${command.address} 未找到")
            return
        }

        val bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothDevice.connectGatt(mActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE)// 第二个参数表示是否自动重连
        } else {
            bluetoothDevice.connectGatt(mActivity, false, mGattCallback)// 第二个参数表示是否自动重连
        }
        if (bluetoothGatt == null) {
            command.failureAndComplete("连接蓝牙设备失败：${command.address}")
            return
        }
        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            disconnect(DisconnectCommand(command.address))
            command.failureAndComplete("连接超时：${command.address}")
        })
    }

    @Synchronized
    override fun disconnect(command: DisconnectCommand) {
        val curCommand = mCurCommand
        if (curCommand != null && curCommand !is DisconnectCommand) {
            curCommand.failureAndComplete("主动断开连接：${command.address}")
        }

        mCurCommand = command

        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.successAndComplete()
            return
        }
        bluetoothGatt.disconnect()
    }

    override fun writeAndWaitForData(command: WriteAndWaitForDataCommand) {
        mCurCommand = command
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        if (command.getBatchDataList().isEmpty()) {
            command.failureAndComplete("没有数据，无法写入：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        val characteristic = bluetoothGatt.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndComplete("特征值不存在：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            command.failureAndComplete("this characteristic not support write and notify")
            return
        }

        /*
        写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
        WRITE_TYPE_DEFAULT  默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
        WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
        WRITE_TYPE_SIGNED  写特征携带认证签名，具体作用不太清楚。
        */
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            command.getBatchDataList().forEach {
                characteristic.value = it
                if (!bluetoothGatt.writeCharacteristic(characteristic)) {
                    command.failureAndComplete("写特征值失败：${getUuidValidString(command.characteristicUuidString)}")
                    return@launch
                }
                delay(command.writeInterval)
            }
        })

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndComplete("写数据并获取通知数据超时：${getUuidValidString(command.characteristicUuidString)}")
        })
    }

    override fun readCharacteristic(command: ReadCharacteristicCommand) {
        mCurCommand = command
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        val characteristic = bluetoothGatt.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndComplete("特征值不存在：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            command.failureAndComplete("this characteristic not support read!")
            return
        }

        if (!bluetoothGatt.readCharacteristic(characteristic)) {
            command.failureAndComplete("读取特征值失败：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndComplete("读取特征值超时：${getUuidValidString(command.characteristicUuidString)}")
        })
    }

    override fun writeCharacteristic(command: WriteCharacteristicCommand) {
        mCurCommand = command
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        if (command.getBatchDataList().isEmpty()) {
            command.failureAndComplete("没有数据，无法写入：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        val characteristic = bluetoothGatt.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndComplete("特征值不存在：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0) {
            command.failureAndComplete("this characteristic not support write!")
            return
        }

        /*
        写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
        WRITE_TYPE_DEFAULT  默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
        WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
        WRITE_TYPE_SIGNED  写特征携带认证签名，具体作用不太清楚。
        */
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            command.getBatchDataList().forEach {
                characteristic.value = it
                if (!bluetoothGatt.writeCharacteristic(characteristic)) {
                    command.failureAndComplete("写特征值失败：${getUuidValidString(command.characteristicUuidString)}")
                    return@launch
                }
                delay(command.writeInterval)// todo 不用延时，用onCharacteristicWrite()方法来触发下一次写。可以考虑宏命令，把分包变成多个写命令。
            }
        })

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndComplete("写特征值超时：${getUuidValidString(command.characteristicUuidString)}")
        })
    }

    override fun setMtu(command: SetMtuCommand) {
        mCurCommand = command
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            command.failureAndComplete("android 5.0及其以上才支持设置MTU：${command.address}")
            return
        }

        if (!bluetoothGatt.requestMtu(command.mtu)) {
            command.failureAndComplete("设置MTU失败：${command.address}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndComplete("设置MTU超时：${command.address}")
        })
    }

    override fun readRemoteRssi(command: ReadRemoteRssiCommand) {
        mCurCommand = command
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        if (!bluetoothGatt.readRemoteRssi()) {
            command.failureAndComplete("读RSSI失败：${command.address}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndComplete("读RSSI超时：${command.address}")
        })
    }

    override fun requestConnectionPriority(command: RequestConnectionPriorityCommand) {
        mCurCommand = command
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            command.failureAndComplete("android 5.0及其以上才支持requestConnectionPriority：${command.address}")
            return
        }

        if (!bluetoothGatt.requestConnectionPriority(command.connectionPriority)) {
            command.failureAndComplete("requestConnectionPriority失败：${command.address}")
        } else {
            command.successAndComplete()
        }
    }

    override fun enableCharacteristicNotify(command: EnableCharacteristicNotifyCommand) {
        mCurCommand = command
        setCharacteristicNotification(command.characteristicUuidString, command.descriptorUuidString, true, command)
    }

    override fun disableCharacteristicNotify(command: DisableCharacteristicNotifyCommand) {
        mCurCommand = command
        setCharacteristicNotification(command.characteristicUuidString, command.descriptorUuidString, false, command)
    }

    override fun enableCharacteristicIndicate(command: EnableCharacteristicIndicateCommand) {
        mCurCommand = command
        setCharacteristicIndication(command.characteristicUuidString, command.descriptorUuidString, true, command)
    }

    override fun disableCharacteristicIndicate(command: DisableCharacteristicIndicateCommand) {
        mCurCommand = command
        setCharacteristicIndication(command.characteristicUuidString, command.descriptorUuidString, false, command)
    }

    private fun setCharacteristicNotification(
        characteristicUuidString: String,
        descriptorUuidString: String,
        enable: Boolean,
        command: Command
    ) {
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接")
            return
        }

        val characteristic = bluetoothGatt.findCharacteristic(characteristicUuidString)
        if (characteristic == null) {
            command.failureAndComplete("特征值不存在：${getUuidValidString(characteristicUuidString)}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            command.failureAndComplete("this characteristic not support notify!")
            return
        }

        if (!bluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            command.failureAndComplete("setCharacteristicNotification fail")
            return
        }

        val descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUuidString))
        if (descriptor == null) {
            command.failureAndComplete("descriptor equals null")
            return
        }

        descriptor.value = if (enable) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        if (!bluetoothGatt.writeDescriptor(descriptor)) {
            command.failureAndComplete("gatt writeDescriptor fail")
            return
        }
    }

    private fun setCharacteristicIndication(
        characteristicUuidString: String,
        descriptorUuidString: String,
        enable: Boolean,
        command: Command
    ) {
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接")
            return
        }

        val characteristic = bluetoothGatt.findCharacteristic(characteristicUuidString)
        if (characteristic == null) {
            command.failureAndComplete("特征值不存在：${getUuidValidString(characteristicUuidString)}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == 0) {
            command.failureAndComplete("this characteristic not support indicate!")
            return
        }

        if (!bluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            command.failureAndComplete("setCharacteristicNotification fail")
            return
        }

        val descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUuidString))
        if (descriptor == null) {
            command.failureAndComplete("descriptor equals null")
            return
        }

        descriptor.value = if (enable) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        if (!bluetoothGatt.writeDescriptor(descriptor)) {
            command.failureAndComplete("gatt writeDescriptor fail")
            return
        }
    }

    @Synchronized
    override fun close(command: CloseCommand) {
        mBluetoothGatt?.device?.address?.let {
            disconnect(DisconnectCommand(it))
        }
        mBluetoothGatt?.close()
        mBluetoothGatt = null
        mCurCommand = null
        command.successAndComplete()
    }

}