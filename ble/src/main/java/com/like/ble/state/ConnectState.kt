package com.like.ble.state

import android.bluetooth.*
import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.*
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
    // 缓存需要回调的命令
    private val mCommandCaches = mutableMapOf<String, Command>()
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
                        notifyDisconnectedAndClearCache()
                        mBluetoothGatt?.close()
                        mBluetoothGatt = null
                    }
                }
            } else {
                notifyDisconnectedAndClearCache()
                mBluetoothGatt?.close()
                mBluetoothGatt = null
            }
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                getCache<ConnectCommand>()?.successAndCompleteIfIncomplete()
            } else {
                gatt.disconnect()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            getCache<WriteAndWaitForDataCommand>()?.let {
                it.addDataToCache(characteristic.value)
                if (it.isWholeFrame()) {
                    it.successAndCompleteIfIncomplete()
                }
            }
        }

        // 谁进行读数据操作，然后外围设备才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            getCache<ReadCharacteristicCommand>()?.let {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    it.addDataToCache(characteristic.value)
                    if (it.isWholeFrame()) {
                        it.successAndCompleteIfIncomplete()
                    }
                } else {
                    it.failureAndCompleteIfIncomplete("读取特征值失败：${characteristic.uuid.getValidString()}")
                }
            }
        }

        // 写特征值，注意，这里的characteristic.value中的数据是你写入的数据，而不是外围设备sendResponse返回的。
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            getCache<WriteCharacteristicCommand>()?.let {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (it.isAllWrite()) {
                        it.successAndCompleteIfIncomplete()
                    }
                } else {
                    it.failureAndCompleteIfIncomplete("写特征值失败：${characteristic.uuid.getValidString()}")
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            mCommandCaches.values.forEach {
                if (it is EnableCharacteristicNotifyCommand ||
                    it is DisableCharacteristicNotifyCommand ||
                    it is EnableCharacteristicIndicateCommand ||
                    it is DisableCharacteristicIndicateCommand
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        it.successAndCompleteIfIncomplete()
                    } else {
                        it.failureAndCompleteIfIncomplete("writeDescriptor fail")
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            getCache<SetMtuCommand>()?.let {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    it.successAndCompleteIfIncomplete(mtu)
                } else {
                    it.failureAndCompleteIfIncomplete("failed to set mtu")
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            getCache<ReadRemoteRssiCommand>()?.let {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    it.successAndCompleteIfIncomplete(rssi)
                } else {
                    it.failureAndCompleteIfIncomplete("failed to read remote rssi")
                }
            }
        }

    }

    @Synchronized
    override fun connect(command: ConnectCommand) {
        if (isConnected()) {
            command.successAndCompleteIfIncomplete()
            return
        }

        // 获取远端的蓝牙设备
        val bluetoothDevice = mActivity.getBluetoothAdapter()?.getRemoteDevice(command.address)
        if (bluetoothDevice == null) {
            command.failureAndCompleteIfIncomplete("连接蓝牙设备失败：设备 ${command.address} 未找到")
            return
        }

        addCache(command)
        mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothDevice.connectGatt(mActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE)// 第二个参数表示是否自动重连
        } else {
            bluetoothDevice.connectGatt(mActivity, false, mGattCallback)// 第二个参数表示是否自动重连
        }
        if (mBluetoothGatt == null) {
            command.failureAndCompleteIfIncomplete("连接蓝牙设备失败：${command.address}")
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
            mBluetoothGatt?.close()
        }
        mBluetoothGatt = null
        notifyDisconnectedAndClearCache()
        command.completeIfIncomplete()
    }

    override fun writeAndWaitForData(command: WriteAndWaitForDataCommand) {
        if (!isConnected()) {
            command.failureAndCompleteIfIncomplete("设备未连接：${command.address}")
            return
        }

        if (command.getBatchDataList().isEmpty()) {
            command.failureAndCompleteIfIncomplete("没有数据，无法写入：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndCompleteIfIncomplete("特征值不存在：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            command.failureAndCompleteIfIncomplete("this characteristic not support write and notify")
            return
        }

        addCache(command)

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            /*
            写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
            WRITE_TYPE_DEFAULT  默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
            WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
            WRITE_TYPE_SIGNED  写特征携带认证签名，具体作用不太清楚。
            */
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            command.getBatchDataList().forEach {
                characteristic.value = it
                if (mBluetoothGatt?.writeCharacteristic(characteristic) != true) {
                    command.failureAndCompleteIfIncomplete("写特征值失败：${getUuidValidString(command.characteristicUuidString)}")
                    return@launch
                }
                delay(command.writeInterval)
            }
        })

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndCompleteIfIncomplete("写数据并获取通知数据超时：${getUuidValidString(command.characteristicUuidString)}")
        })
    }

    override fun readCharacteristic(command: ReadCharacteristicCommand) {
        if (!isConnected()) {
            command.failureAndCompleteIfIncomplete("设备未连接：${command.address}")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndCompleteIfIncomplete("特征值不存在：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            command.failureAndCompleteIfIncomplete("this characteristic not support read!")
            return
        }

        addCache(command)

        if (mBluetoothGatt?.readCharacteristic(characteristic) != true) {
            command.failureAndCompleteIfIncomplete("读取特征值失败：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndCompleteIfIncomplete("读取特征值超时：${getUuidValidString(command.characteristicUuidString)}")
        })
    }

    override fun writeCharacteristic(command: WriteCharacteristicCommand) {
        if (!isConnected()) {
            command.failureAndCompleteIfIncomplete("设备未连接：${command.address}")
            return
        }

        if (command.getBatchDataList().isEmpty()) {
            command.failureAndCompleteIfIncomplete("没有数据，无法写入：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndCompleteIfIncomplete("特征值不存在：${getUuidValidString(command.characteristicUuidString)}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0) {
            command.failureAndCompleteIfIncomplete("this characteristic not support write!")
            return
        }

        addCache(command)

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            /*
            写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
            WRITE_TYPE_DEFAULT  默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
            WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
            WRITE_TYPE_SIGNED  写特征携带认证签名，具体作用不太清楚。
            */
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            command.getBatchDataList().forEach {
                characteristic.value = it
                if (mBluetoothGatt?.writeCharacteristic(characteristic) != true) {
                    command.failureAndCompleteIfIncomplete("写特征值失败：${getUuidValidString(command.characteristicUuidString)}")
                    return@launch
                }
                delay(command.writeInterval)// todo 不用延时，用onCharacteristicWrite()方法来触发下一次写。可以考虑宏命令，把分包变成多个写命令。
            }
        })

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndCompleteIfIncomplete("写特征值超时：${getUuidValidString(command.characteristicUuidString)}")
        })
    }

    override fun setMtu(command: SetMtuCommand) {
        if (!isConnected()) {
            command.failureAndCompleteIfIncomplete("设备未连接：${command.address}")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            command.failureAndCompleteIfIncomplete("android 5.0及其以上才支持设置MTU：${command.address}")
            return
        }

        addCache(command)
        if (mBluetoothGatt?.requestMtu(command.mtu) != true) {
            command.failureAndCompleteIfIncomplete("设置MTU失败：${command.address}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndCompleteIfIncomplete("设置MTU超时：${command.address}")
        })
    }

    override fun readRemoteRssi(command: ReadRemoteRssiCommand) {
        if (!isConnected()) {
            command.failureAndCompleteIfIncomplete("设备未连接：${command.address}")
            return
        }

        addCache(command)
        if (mBluetoothGatt?.readRemoteRssi() != true) {
            command.failureAndCompleteIfIncomplete("读RSSI失败：${command.address}")
            return
        }

        command.addJob(mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndCompleteIfIncomplete("读RSSI超时：${command.address}")
        })
    }

    override fun requestConnectionPriority(command: RequestConnectionPriorityCommand) {
        if (!isConnected()) {
            command.failureAndCompleteIfIncomplete("设备未连接：${command.address}")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            command.failureAndCompleteIfIncomplete("android 5.0及其以上才支持requestConnectionPriority：${command.address}")
            return
        }

        addCache(command)
        if (mBluetoothGatt?.requestConnectionPriority(command.connectionPriority) != true) {
            command.failureAndCompleteIfIncomplete("requestConnectionPriority失败：${command.address}")
        } else {
            command.successAndCompleteIfIncomplete()
        }
    }

    override fun enableCharacteristicNotify(command: EnableCharacteristicNotifyCommand) {
        setCharacteristicNotification(command.characteristicUuidString, command.descriptorUuidString, true, command)
    }

    override fun disableCharacteristicNotify(command: DisableCharacteristicNotifyCommand) {
        setCharacteristicNotification(command.characteristicUuidString, command.descriptorUuidString, false, command)
    }

    override fun enableCharacteristicIndicate(command: EnableCharacteristicIndicateCommand) {
        setCharacteristicIndication(command.characteristicUuidString, command.descriptorUuidString, true, command)
    }

    override fun disableCharacteristicIndicate(command: DisableCharacteristicIndicateCommand) {
        setCharacteristicIndication(command.characteristicUuidString, command.descriptorUuidString, false, command)
    }

    private fun setCharacteristicNotification(
        characteristicUuidString: String,
        descriptorUuidString: String,
        enable: Boolean,
        command: Command
    ) {
        if (!isConnected()) {
            command.failureAndCompleteIfIncomplete("设备未连接")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuidString)
        if (characteristic == null) {
            command.failureAndCompleteIfIncomplete("特征值不存在：${getUuidValidString(characteristicUuidString)}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            command.failureAndCompleteIfIncomplete("this characteristic not support notify!")
            return
        }

        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
            command.failureAndCompleteIfIncomplete("setCharacteristicNotification fail")
            return
        }

        val descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUuidString))
        if (descriptor == null) {
            command.failureAndCompleteIfIncomplete("descriptor equals null")
            return
        }

        descriptor.value = if (enable) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        addCache(command)

        if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
            command.failureAndCompleteIfIncomplete("gatt writeDescriptor fail")
            return
        }
    }

    private fun setCharacteristicIndication(
        characteristicUuidString: String,
        descriptorUuidString: String,
        enable: Boolean,
        command: Command
    ) {
        if (!isConnected()) {
            command.failureAndCompleteIfIncomplete("设备未连接")
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuidString)
        if (characteristic == null) {
            command.failureAndCompleteIfIncomplete("特征值不存在：${getUuidValidString(characteristicUuidString)}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == 0) {
            command.failureAndCompleteIfIncomplete("this characteristic not support indicate!")
            return
        }

        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
            command.failureAndCompleteIfIncomplete("setCharacteristicNotification fail")
            return
        }

        val descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUuidString))
        if (descriptor == null) {
            command.failureAndCompleteIfIncomplete("descriptor equals null")
            return
        }

        descriptor.value = if (enable) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        addCache(command)

        if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
            command.failureAndCompleteIfIncomplete("gatt writeDescriptor fail")
            return
        }
    }

    @Synchronized
    override fun close(command: CloseCommand) {
        disconnect(DisconnectCommand(command.address))
        command.completeIfIncomplete()
    }

    private fun addCache(command: Command) {
        mCommandCaches[command::class.java.simpleName] = command
    }

    private inline fun <reified T : Command> getCache(): T? {
        val key = T::class.java.simpleName
        return if (mCommandCaches.containsKey(key)) {
            mCommandCaches[key] as? T
        } else {
            null
        }
    }

    private fun notifyDisconnectedAndClearCache() {
        mCommandCaches.values.forEach {
            if (it is ConnectCommand) {
                it.failureAndComplete("蓝牙连接失败：${it.address}")
            } else {
                it.failureAndCompleteIfIncomplete("蓝牙已断开：${it.address}")
            }
        }
        mCommandCaches.clear()
    }

    private fun isConnected(): Boolean {
        val device = mBluetoothGatt?.device ?: return false
        return mActivity.getBluetoothManager()?.getConnectedDevices(BluetoothProfile.GATT)?.any {
            it == device
        } ?: false
    }

}