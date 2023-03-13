package com.like.ble.central.executor

import android.annotation.SuppressLint
import android.bluetooth.*
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.central.command.*
import com.like.ble.command.Command
import com.like.ble.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 蓝牙连接及连接成功后的命令执行者
 * 可以进行连接、断开连接、操作数据等等操作
 */
@SuppressLint("MissingPermission")
class ConnectCommandExecutor(private val mActivity: ComponentActivity) : CentralCommandExecutor() {
    private var mBluetoothGatt: BluetoothGatt? = null

    // 蓝牙Gatt回调方法中都不可以进行耗时操作，需要将其方法内进行的操作丢进另一个线程，尽快返回。
    private val mGattCallback = object : BluetoothGattCallback() {
        // 当连接状态改变
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    // 连接蓝牙设备成功
                    gatt.discoverServices()
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    // 断开连接蓝牙设备成功
                    disconnectCommand?.complete()
                    closeBluetoothGatt()
                }
            } else {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    disconnectCommand?.error("断开连接蓝牙失败：${gatt.device.address}")
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    connectCommand?.error("连接蓝牙失败：${gatt.device.address}")
                    closeBluetoothGatt()
                }
            }
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                connectCommand?.result(gatt.services)
            } else {
                connectCommand?.error("连接蓝牙失败：${gatt.device.address}")
                disconnectBluetoothGatt()
                closeBluetoothGatt()
            }
        }

        // 谁进行读数据操作，然后外围设备才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val command = readCharacteristicCommand ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.result(characteristic.value)
            } else {
                command.error("读取特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        // 写特征值，注意，这里的characteristic.value中的数据是你写入的数据，而不是外围设备sendResponse返回的。
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val command = writeCharacteristicCommand ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (command.isAllWrite()) {
                    command.complete()
                }
            } else {
                command.error("写特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val command = readNotifyCommand ?: return
            if (command.addDataToCache(characteristic.value)) {
                if (command.isWholeFrame()) {
                    command.result(command.getData())
                }
            } else {
                command.error("添加数据到缓存失败")
            }
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val command = readDescriptorCommand ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.result(descriptor.value)
            } else {
                command.error("读取描述值失败：${descriptor.uuid.getValidString()}")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val command = writeDescriptorCommand ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (command.isAllWrite()) {
                    command.complete()
                }
            } else {
                command.error("写描述值失败：${descriptor.uuid.getValidString()}")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val command = requestMtuCommand ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.result(mtu)
            } else {
                command.error("failed to set mtu")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            val command = readRemoteRssiCommand ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                command.result(rssi)
            } else {
                command.error("failed to read remote rssi")
            }
        }

    }

    @Synchronized
    override fun connect(command: ConnectCommand) {
        if (isConnected()) {
            command.result(mBluetoothGatt?.services)
            return
        }

        // 获取远端的蓝牙设备
        val bluetoothDevice = mActivity.getBluetoothAdapter()?.getRemoteDevice(command.address)
        if (bluetoothDevice == null) {
            command.error("连接蓝牙失败：${command.address} 未找到")
            return
        }

        mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothDevice.connectGatt(
                mActivity,
                false,
                mGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )// 第二个参数表示是否自动重连
        } else {
            bluetoothDevice.connectGatt(mActivity, false, mGattCallback)// 第二个参数表示是否自动重连
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.error("连接蓝牙超时：${command.address}")
            disconnectBluetoothGatt()
            closeBluetoothGatt()
        })
    }

    @Synchronized
    override fun disconnect(command: DisconnectCommand) {
        disconnectBluetoothGatt()

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.error("断开连接蓝牙超时：${command.address}")
            closeBluetoothGatt()
        })
    }

    override fun readCharacteristic(command: ReadCharacteristicCommand) {
        if (!isConnected()) {
            command.error("蓝牙未连接：${command.address}")
            return
        }

        val characteristic =
            mBluetoothGatt?.findCharacteristic(command.characteristicUuid, command.serviceUuid)
        if (characteristic == null) {
            command.error("特征值不存在：${command.characteristicUuid.getValidString()}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            command.error("this characteristic not support read!")
            return
        }

        if (mBluetoothGatt?.readCharacteristic(characteristic) != true) {
            command.error("读取特征值失败：${command.characteristicUuid.getValidString()}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.error("读取特征值超时：${command.characteristicUuid.getValidString()}")
        })
    }

    override fun writeCharacteristic(command: WriteCharacteristicCommand) {
        if (!isConnected()) {
            command.error("蓝牙未连接：${command.address}")
            return
        }

        val characteristic =
            mBluetoothGatt?.findCharacteristic(command.characteristicUuid, command.serviceUuid)
        if (characteristic == null) {
            command.error("特征值不存在：${command.characteristicUuid.getValidString()}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0) {
            command.error("this characteristic not support write!")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            /*
            写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
            WRITE_TYPE_DEFAULT 默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
            WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
            WRITE_TYPE_SIGNED 写特征携带认证签名，具体作用不太清楚。
            */
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            command.data.forEach {
                characteristic.value = it
                if (mBluetoothGatt?.writeCharacteristic(characteristic) != true) {
                    command.error("写特征值失败：${command.characteristicUuid.getValidString()}")
                    return@launch
                }
                command.waitForNextFlag()
                delay(10)
            }
        })

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.error("写特征值超时：${command.characteristicUuid.getValidString()}")
        })
    }

    override fun readDescriptor(command: ReadDescriptorCommand) {
        if (!isConnected()) {
            command.error("蓝牙未连接：${command.address}")
            return
        }

        val descriptor = mBluetoothGatt?.findDescriptor(
            command.descriptorUuid,
            command.characteristicUuid,
            command.serviceUuid
        )
        if (descriptor == null) {
            command.error("描述值不存在：${command.descriptorUuid.getValidString()}")
            return
        }

        // 由于descriptor.permissions永远为0x0000，所以无法判断，但是如果权限不允许，还是会操作失败的。
//        if (descriptor.permissions and (BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) == 0) {
//            command.error("this descriptor not support read!")
//            return
//        }

        if (mBluetoothGatt?.readDescriptor(descriptor) != true) {
            command.error("读取描述值失败：${command.descriptorUuid.getValidString()}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.error("读取描述值超时：${command.descriptorUuid.getValidString()}")
        })
    }

    override fun writeDescriptor(command: WriteDescriptorCommand) {
        if (!isConnected()) {
            command.error("蓝牙未连接：${command.address}")
            return
        }

        val descriptor = mBluetoothGatt?.findDescriptor(
            command.descriptorUuid,
            command.characteristicUuid,
            command.serviceUuid
        )
        if (descriptor == null) {
            command.error("描述值不存在：${command.descriptorUuid.getValidString()}")
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
//            command.error("this descriptor not support write!")
//            return
//        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            command.data.forEach {
                descriptor.value = it
                if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
                    command.error("写描述值失败：${command.descriptorUuid.getValidString()}")
                    return@launch
                }
                command.waitForNextFlag()
                delay(10)
            }
        })

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.error("写描述值超时：${command.descriptorUuid.getValidString()}")
        })
    }

    override fun readNotify(command: ReadNotifyCommand) {
        if (!isConnected()) {
            command.error("蓝牙未连接：${command.address}")
            return
        }

        val characteristic =
            mBluetoothGatt?.findCharacteristic(command.characteristicUuid, command.serviceUuid)
        if (characteristic == null) {
            command.error("特征值不存在：${command.characteristicUuid.getValidString()}")
            return
        }

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_INDICATE or BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            command.error("this characteristic not support indicate or notify")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.error("读取通知传来的数据超时：${command.characteristicUuid.getValidString()}")
        })

    }

    override fun requestMtu(command: RequestMtuCommand) {
        if (!isConnected()) {
            command.error("蓝牙未连接：${command.address}")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            command.error("android 5.0及其以上才支持设置MTU：${command.address}")
            return
        }

        if (mBluetoothGatt?.requestMtu(command.mtu) != true) {
            command.error("设置MTU失败：${command.address}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.error("设置MTU超时：${command.address}")
        })
    }

    override fun readRemoteRssi(command: ReadRemoteRssiCommand) {
        if (!isConnected()) {
            command.error("蓝牙未连接：${command.address}")
            return
        }

        if (mBluetoothGatt?.readRemoteRssi() != true) {
            command.error("读RSSI失败：${command.address}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.error("读RSSI超时：${command.address}")
        })
    }

    override fun requestConnectionPriority(command: RequestConnectionPriorityCommand) {
        if (!isConnected()) {
            command.error("蓝牙未连接：${command.address}")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            command.error("android 5.0及其以上才支持requestConnectionPriority：${command.address}")
            return
        }

        if (mBluetoothGatt?.requestConnectionPriority(command.connectionPriority) != true) {
            command.error("requestConnectionPriority失败：${command.address}")
        } else {
            command.result(command.connectionPriority)
        }
    }

    override fun setCharacteristicNotification(command: SetCharacteristicNotificationCommand) {
        if (!isConnected()) {
            command.error("蓝牙未连接")
            return
        }

        val characteristic =
            mBluetoothGatt?.findCharacteristic(command.characteristicUuid, command.serviceUuid)
        if (characteristic == null) {
            command.error("特征值不存在：${command.characteristicUuid.getValidString()}")
            return
        }

        when (command.type) {
            SetCharacteristicNotificationCommand.TYPE_NOTIFY -> {
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
                    command.error("this characteristic not support notify!")
                    return
                }
            }
            SetCharacteristicNotificationCommand.TYPE_INDICATE -> {
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == 0) {
                    command.error("this characteristic not support indicate!")
                    return
                }
            }
        }

        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, command.enable) != true) {
            command.error("setCharacteristicNotification fail")
            return
        }

        command.complete()
    }

    private fun setNotification(
        serviceUuid: UUID?,
        characteristicUuid: UUID,
        descriptorUuid: UUID,
        enable: Boolean,
        command: Command
    ) {
        if (!isConnected()) {
            command.error("蓝牙未连接")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            command.error("特征值不存在：${characteristicUuid.getValidString()}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            command.error("this characteristic not support notify!")
            return
        }

        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
            command.error("setCharacteristicNotification fail")
            return
        }

        val descriptor = characteristic.getDescriptor(descriptorUuid)
        if (descriptor == null) {
            command.error("descriptor equals null")
            return
        }

        descriptor.value = if (enable) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
            command.error("gatt writeDescriptor fail")
            return
        }

        command.complete()
    }

    private fun setIndication(
        serviceUuid: UUID?,
        characteristicUuid: UUID,
        descriptorUuid: UUID,
        enable: Boolean,
        command: Command
    ) {
        if (!isConnected()) {
            command.error("设备未连接")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            command.error("特征值不存在：${characteristicUuid.getValidString()}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == 0) {
            command.error("this characteristic not support indicate!")
            return
        }

        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
            command.error("setCharacteristicNotification fail")
            return
        }

        val descriptor = characteristic.getDescriptor(descriptorUuid)
        if (descriptor == null) {
            command.error("descriptor equals null")
            return
        }

        descriptor.value = if (enable) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
            command.error("gatt writeDescriptor fail")
            return
        }

        command.complete()
    }

    @Synchronized
    override fun close() {
        disconnectBluetoothGatt()
        closeBluetoothGatt()
        super.close()
    }

    private fun disconnectBluetoothGatt() {
        if (isConnected()) {
            mBluetoothGatt?.disconnect()
        }
    }

    private fun closeBluetoothGatt() {
        // 这里的close()方法会清空mGattCallback，导致收不到回调
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    private fun isConnected(): Boolean {
        val device = mBluetoothGatt?.device ?: return false
        return mActivity.isBleDeviceConnected(device)
    }

}