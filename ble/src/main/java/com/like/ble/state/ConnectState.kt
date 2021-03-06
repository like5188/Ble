package com.like.ble.state

import android.bluetooth.*
import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.*
import com.like.ble.command.base.Command
import com.like.ble.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 蓝牙连接状态
 * 可以进行连接、操作数据等等操作
 */
class ConnectState(private val mActivity: FragmentActivity) : State() {
    private var mBluetoothGatt: BluetoothGatt? = null
    // 命令缓存
    private val mCommands = mutableMapOf<String, Command>()
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
                        disconnect(DisconnectCommand(gatt.device.address))
                    }
                }
            } else {
                disconnect(DisconnectCommand(gatt.device.address))
            }
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                getCommandFromCache<ConnectCommand>()?.resultAndComplete(gatt.services)
            } else {
                disconnect(DisconnectCommand(gatt.device.address))
            }
        }

        // 谁进行读数据操作，然后外围设备才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val command = getCommandFromCache<ReadCharacteristicCommand>() ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.resultAndComplete(characteristic.value)
            } else {
                command.errorAndComplete("读取特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        // 写特征值，注意，这里的characteristic.value中的数据是你写入的数据，而不是外围设备sendResponse返回的。
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val command = getCommandFromCache<WriteCharacteristicCommand>() ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (command.isAllWrite()) {
                    command.complete()
                }
            } else {
                command.errorAndComplete("写特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val command = getCommandFromCache<ReadNotifyCommand>() ?: return
            if (command.addDataToCache(characteristic.value)) {
                if (command.isWholeFrame()) {
                    command.resultAndComplete(command.getData())
                }
            } else {
                command.errorAndComplete("添加数据到缓存失败")
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            val command = getCommandFromCache<ReadDescriptorCommand>() ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.resultAndComplete(descriptor.value)
            } else {
                command.errorAndComplete("读取描述值失败：${descriptor.uuid.getValidString()}")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            val command = getCommandFromCache<WriteDescriptorCommand>() ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (command.isAllWrite()) {
                    command.complete()
                }
            } else {
                command.errorAndComplete("写描述值失败：${descriptor.uuid.getValidString()}")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val command = getCommandFromCache<RequestMtuCommand>() ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.resultAndComplete(mtu)
            } else {
                command.errorAndComplete("failed to set mtu")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            val command = getCommandFromCache<ReadRemoteRssiCommand>() ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.resultAndComplete(rssi)
            } else {
                command.errorAndComplete("failed to read remote rssi")
            }
        }

    }

    @Synchronized
    override fun connect(command: ConnectCommand) {
        if (isConnected()) {
            command.errorAndComplete("蓝牙已经连接了")
            return
        }

        // 获取远端的蓝牙设备
        val bluetoothDevice = mActivity.getBluetoothAdapter()?.getRemoteDevice(command.address)
        if (bluetoothDevice == null) {
            command.errorAndComplete("连接蓝牙设备失败：远程设备 ${command.address} 未找到")
            return
        }

        addCommandToCache(command)

        mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothDevice.connectGatt(mActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE)// 第二个参数表示是否自动重连
        } else {
            bluetoothDevice.connectGatt(mActivity, false, mGattCallback)// 第二个参数表示是否自动重连
        }
        if (mBluetoothGatt == null) {
            command.errorAndComplete("连接蓝牙设备失败：${command.address}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            disconnect(DisconnectCommand(command.address))
        })
    }

    @Synchronized
    override fun disconnect(command: DisconnectCommand) {
        if (isConnected()) {
            mBluetoothGatt?.disconnect()
            mCommands.forEach {
                val cmd = it.value
                if (cmd is ConnectCommand) {
                    cmd.errorAndComplete("蓝牙连接断开了")
                } else {
                    cmd.errorAndComplete("蓝牙连接断开了")
                }
            }
            command.complete()
        } else {
            mCommands.forEach {
                val cmd = it.value
                if (cmd is ConnectCommand) {
                    cmd.errorAndComplete("蓝牙未连接")
                } else {
                    cmd.errorAndComplete("蓝牙未连接")
                }
            }
            command.errorAndComplete("蓝牙未连接")
        }
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    override fun readCharacteristic(command: ReadCharacteristicCommand) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接：${command.address}")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuid, command.serviceUuid)
        if (characteristic == null) {
            command.errorAndComplete("特征值不存在：${command.characteristicUuid.getValidString()}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            command.errorAndComplete("this characteristic not support read!")
            return
        }

        addCommandToCache(command)

        if (mBluetoothGatt?.readCharacteristic(characteristic) != true) {
            command.errorAndComplete("读取特征值失败：${command.characteristicUuid.getValidString()}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.errorAndComplete("读取特征值超时：${command.characteristicUuid.getValidString()}")
        })
    }

    override fun writeCharacteristic(command: WriteCharacteristicCommand) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接：${command.address}")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuid, command.serviceUuid)
        if (characteristic == null) {
            command.errorAndComplete("特征值不存在：${command.characteristicUuid.getValidString()}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0) {
            command.errorAndComplete("this characteristic not support write!")
            return
        }

        addCommandToCache(command)

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            /*
            写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
            WRITE_TYPE_DEFAULT  默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
            WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
            WRITE_TYPE_SIGNED  写特征携带认证签名，具体作用不太清楚。
            */
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            command.data.forEach {
                characteristic.value = it
                if (mBluetoothGatt?.writeCharacteristic(characteristic) != true) {
                    command.errorAndComplete("写特征值失败：${command.characteristicUuid.getValidString()}")
                    return@launch
                }
                command.waitForNextFlag()
                delay(10)
            }
        })

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.errorAndComplete("写特征值超时：${command.characteristicUuid.getValidString()}")
        })
    }

    override fun readDescriptor(command: ReadDescriptorCommand) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接：${command.address}")
            return
        }

        val descriptor = mBluetoothGatt?.findDescriptor(command.descriptorUuid, command.characteristicUuid, command.serviceUuid)
        if (descriptor == null) {
            command.errorAndComplete("描述值不存在：${command.descriptorUuid.getValidString()}")
            return
        }

        // 由于descriptor.permissions永远为0x0000，所以无法判断，但是如果权限不允许，还是会操作失败的。
//        if (descriptor.permissions and (BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) == 0) {
//            command.errorAndComplete("this descriptor not support read!")
//            return
//        }

        addCommandToCache(command)

        if (mBluetoothGatt?.readDescriptor(descriptor) != true) {
            command.errorAndComplete("读取描述值失败：${command.descriptorUuid.getValidString()}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.errorAndComplete("读取描述值超时：${command.descriptorUuid.getValidString()}")
        })
    }

    override fun writeDescriptor(command: WriteDescriptorCommand) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接：${command.address}")
            return
        }

        val descriptor = mBluetoothGatt?.findDescriptor(command.descriptorUuid, command.characteristicUuid, command.serviceUuid)
        if (descriptor == null) {
            command.errorAndComplete("描述值不存在：${command.descriptorUuid.getValidString()}")
            return
        }

        // 由于descriptor.permissions永远为0x0000，所以无法判断，但是如果权限不允许，还是会操作失败的。
//        if (descriptor.permissions and
//            (BluetoothGattDescriptor.PERMISSION_WRITE or
//                    BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED or
//                    BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM or
//                    BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED or
//                    BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM
//                    ) == 0
//        ) {
//            command.errorAndComplete("this descriptor not support write!")
//            return
//        }

        addCommandToCache(command)

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            command.data.forEach {
                descriptor.value = it
                if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
                    command.errorAndComplete("写描述值失败：${command.descriptorUuid.getValidString()}")
                    return@launch
                }
                command.waitForNextFlag()
                delay(10)
            }
        })

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.errorAndComplete("写描述值超时：${command.descriptorUuid.getValidString()}")
        })
    }

    override fun readNotify(command: ReadNotifyCommand) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接：${command.address}")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuid, command.serviceUuid)
        if (characteristic == null) {
            command.errorAndComplete("特征值不存在：${command.characteristicUuid.getValidString()}")
            return
        }

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_INDICATE or BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            command.errorAndComplete("this characteristic not support indicate or notify")
            return
        }

        addCommandToCache(command)

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.errorAndComplete("读取通知传来的数据超时：${command.characteristicUuid.getValidString()}")
        })

    }

    override fun setMtu(command: RequestMtuCommand) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接：${command.address}")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            command.errorAndComplete("android 5.0及其以上才支持设置MTU：${command.address}")
            return
        }

        addCommandToCache(command)

        if (mBluetoothGatt?.requestMtu(command.mtu) != true) {
            command.errorAndComplete("设置MTU失败：${command.address}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.errorAndComplete("设置MTU超时：${command.address}")
        })
    }

    override fun readRemoteRssi(command: ReadRemoteRssiCommand) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接：${command.address}")
            return
        }

        addCommandToCache(command)

        if (mBluetoothGatt?.readRemoteRssi() != true) {
            command.errorAndComplete("读RSSI失败：${command.address}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.errorAndComplete("读RSSI超时：${command.address}")
        })
    }

    override fun requestConnectionPriority(command: RequestConnectionPriorityCommand) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接：${command.address}")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            command.errorAndComplete("android 5.0及其以上才支持requestConnectionPriority：${command.address}")
            return
        }

        addCommandToCache(command)

        if (mBluetoothGatt?.requestConnectionPriority(command.connectionPriority) != true) {
            command.errorAndComplete("requestConnectionPriority失败：${command.address}")
        } else {
            command.resultAndComplete(command.connectionPriority)
        }
    }

    override fun setCharacteristicNotification(command: SetCharacteristicNotificationCommand) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuid, command.serviceUuid)
        if (characteristic == null) {
            command.errorAndComplete("特征值不存在：${command.characteristicUuid.getValidString()}")
            return
        }

        when (command.type) {
            SetCharacteristicNotificationCommand.TYPE_NOTIFY -> {
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
                    command.errorAndComplete("this characteristic not support notify!")
                    return
                }
            }
            SetCharacteristicNotificationCommand.TYPE_INDICATE -> {
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == 0) {
                    command.errorAndComplete("this characteristic not support indicate!")
                    return
                }
            }
        }

        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, command.enable) != true) {
            command.errorAndComplete("setCharacteristicNotification fail")
            return
        }

        command.complete()
    }

    private fun setCharacteristicNotification(
        serviceUuid: UUID?,
        characteristicUuid: UUID,
        descriptorUuid: UUID,
        enable: Boolean,
        command: Command
    ) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            command.errorAndComplete("特征值不存在：${characteristicUuid.getValidString()}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            command.errorAndComplete("this characteristic not support notify!")
            return
        }

        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
            command.errorAndComplete("setCharacteristicNotification fail")
            return
        }

        val descriptor = characteristic.getDescriptor(descriptorUuid)
        if (descriptor == null) {
            command.errorAndComplete("descriptor equals null")
            return
        }

        descriptor.value = if (enable) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        addCommandToCache(command)

        if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
            command.errorAndComplete("gatt writeDescriptor fail")
            return
        }
    }

    private fun setCharacteristicIndication(
        serviceUuid: UUID?,
        characteristicUuid: UUID,
        descriptorUuid: UUID,
        enable: Boolean,
        command: Command
    ) {
        if (!isConnected()) {
            command.errorAndComplete("设备未连接")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            command.errorAndComplete("特征值不存在：${characteristicUuid.getValidString()}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == 0) {
            command.errorAndComplete("this characteristic not support indicate!")
            return
        }

        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
            command.errorAndComplete("setCharacteristicNotification fail")
            return
        }

        val descriptor = characteristic.getDescriptor(descriptorUuid)
        if (descriptor == null) {
            command.errorAndComplete("descriptor equals null")
            return
        }

        descriptor.value = if (enable) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        addCommandToCache(command)

        if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
            command.errorAndComplete("gatt writeDescriptor fail")
            return
        }
    }

    @Synchronized
    override fun close(command: CloseCommand) {
        val address = mBluetoothGatt?.device?.address ?: ""
        if (address.isNotEmpty()) {
            disconnect(DisconnectCommand(address))
        }
        mCommands.clear()
        command.complete()
    }

    private fun isConnected(): Boolean {
        val device = mBluetoothGatt?.device ?: return false
        return mActivity.isBleDeviceConnected(device)
    }

    private fun addCommandToCache(command: Command) {
        mCommands[command::class.java.name] = command
    }

    private inline fun <reified T : Command> getCommandFromCache(): T? {
        return mCommands[T::class.java.name] as? T
    }

}