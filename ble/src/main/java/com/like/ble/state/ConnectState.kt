package com.like.ble.state

import android.bluetooth.*
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.*
import com.like.ble.utils.findCharacteristic
import com.like.ble.utils.getBluetoothAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 蓝牙连接状态
 * 可以进行连接、操作数据等等操作
 */
class ConnectState : State() {
    private var mBluetoothGatt: BluetoothGatt? = null
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
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            mDelayJob?.cancel()
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
                mDelayJob?.cancel()
                curCommand.successAndComplete()
            }
        }

        // 谁进行读数据操作，然后外围设备才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val curCommand = mCurCommand
            if (curCommand !is ReadCharacteristicCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                curCommand.addDataToCache(characteristic.value)
                if (curCommand.isWholeFrame()) {
                    mDelayJob?.cancel()
                    curCommand.successAndComplete()
                }
            } else {
                mDelayJob?.cancel()
                curCommand.failureAndComplete("读取特征值失败：${characteristic.uuid}")
            }
        }

        // 写特征值
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val curCmmand = mCurCommand
            if (curCmmand !is WriteCharacteristicCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (curCmmand.isAllWrite()) {
                    mDelayJob?.cancel()
                    curCmmand.successAndComplete()
                }
            } else {
                mWriteJob?.cancel()
                mDelayJob?.cancel()
                curCmmand.failureAndComplete("写特征值失败：${characteristic.uuid}")
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
                mDelayJob?.cancel()
                curCommand.successAndComplete(mtu)
            } else {
                mDelayJob?.cancel()
                curCommand.failureAndComplete("failed to set mtu")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            val curCommand = mCurCommand
            if (curCommand !is ReadRemoteRssiCommand) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mDelayJob?.cancel()
                curCommand.successAndComplete(rssi)
            } else {
                mDelayJob?.cancel()
                curCommand.failureAndComplete("failed to read remote rssi")
            }
        }

    }

    override fun connect(command: ConnectCommand) {
        super.connect(command)
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
                val bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothDevice.connectGatt(mActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE)// 第二个参数表示是否自动重连
                } else {
                    bluetoothDevice.connectGatt(mActivity, false, mGattCallback)// 第二个参数表示是否自动重连
                }
                if (bluetoothGatt == null) {
                    mDelayJob?.cancel()
                    command.failureAndComplete("连接蓝牙设备失败：${command.address}")
                }
            }

            mDelayJob = launch(Dispatchers.IO) {
                delay(command.timeout)
                disconnect(DisconnectCommand(command.address))
                command.failureAndComplete("连接超时：${command.address}")
            }
        }
    }

    override fun disconnect(command: DisconnectCommand) {
        val curCommand = mCurCommand
        if (curCommand != null && curCommand !is DisconnectCommand) {
            curCommand.failureAndComplete("主动断开连接：${command.address}")
        }
        mDelayJob?.cancel()

        super.disconnect(command)

        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.successAndComplete()
            return
        }
        mActivity.lifecycleScope.launch(Dispatchers.IO) {
            bluetoothGatt.disconnect()
        }
    }

    override fun writeAndWaitForData(command: WriteAndWaitForDataCommand) {
        super.writeAndWaitForData(command)
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        if (command.getBatchDataList().isEmpty()) {
            command.failureAndComplete("没有数据，无法写入：${command.characteristicUuidString}")
            return
        }

        val characteristic = bluetoothGatt.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndComplete("特征值不存在：${command.characteristicUuidString}")
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

        mWriteJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            command.getBatchDataList().forEach {
                characteristic.value = it
                if (!bluetoothGatt.writeCharacteristic(characteristic)) {
                    mDelayJob?.cancel()
                    command.failureAndComplete("写特征值失败：${command.characteristicUuidString}")
                    return@launch
                }
                delay(command.writeInterval)
            }
        }

        mDelayJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            mWriteJob?.cancel()
            command.failureAndComplete("写数据并获取通知数据超时：${command.characteristicUuidString}")
        }
    }

    override fun readCharacteristic(command: ReadCharacteristicCommand) {
        super.readCharacteristic(command)
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        val characteristic = bluetoothGatt.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndComplete("特征值不存在：${command.characteristicUuidString}")
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            command.failureAndComplete("this characteristic not support read!")
            return
        }

        if (!bluetoothGatt.readCharacteristic(characteristic)) {
            command.failureAndComplete("读取特征值失败：${command.characteristicUuidString}")
            return
        }

        mDelayJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndComplete("读取特征值超时：${command.characteristicUuidString}")
        }
    }

    override fun writeCharacteristic(command: WriteCharacteristicCommand) {
        super.writeCharacteristic(command)
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        if (command.getBatchDataList().isEmpty()) {
            command.failureAndComplete("没有数据，无法写入：${command.characteristicUuidString}")
            return
        }

        val characteristic = bluetoothGatt.findCharacteristic(command.characteristicUuidString)
        if (characteristic == null) {
            command.failureAndComplete("特征值不存在：${command.characteristicUuidString}")
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

        mWriteJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            command.getBatchDataList().forEach {
                characteristic.value = it
                if (!bluetoothGatt.writeCharacteristic(characteristic)) {
                    mDelayJob?.cancel()
                    command.failureAndComplete("写特征值失败：${command.characteristicUuidString}")
                    return@launch
                }
                delay(command.writeInterval)
            }
        }

        mDelayJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            mWriteJob?.cancel()
            command.failureAndComplete("写特征值超时：${command.characteristicUuidString}")
        }
    }

    override fun setMtu(command: SetMtuCommand) {
        super.setMtu(command)
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

        mDelayJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndComplete("设置MTU超时：${command.address}")
        }
    }

    override fun readRemoteRssi(command: ReadRemoteRssiCommand) {
        super.readRemoteRssi(command)
        val bluetoothGatt = mBluetoothGatt
        if (bluetoothGatt == null) {
            command.failureAndComplete("设备未连接：${command.address}")
            return
        }

        if (!bluetoothGatt.readRemoteRssi()) {
            command.failureAndComplete("读RSSI失败：${command.address}")
            return
        }

        mDelayJob = mActivity.lifecycleScope.launch(Dispatchers.IO) {
            delay(command.timeout)
            command.failureAndComplete("读RSSI超时：${command.address}")
        }
    }

    override fun requestConnectionPriority(command: RequestConnectionPriorityCommand) {
        super.requestConnectionPriority(command)
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
        super.enableCharacteristicNotify(command)
        setCharacteristicNotification(command.characteristicUuidString, command.descriptorUuidString, true, command)
    }

    override fun disableCharacteristicNotify(command: DisableCharacteristicNotifyCommand) {
        super.disableCharacteristicNotify(command)
        setCharacteristicNotification(command.characteristicUuidString, command.descriptorUuidString, false, command)
    }

    override fun enableCharacteristicIndicate(command: EnableCharacteristicIndicateCommand) {
        super.enableCharacteristicIndicate(command)
        setCharacteristicIndication(command.characteristicUuidString, command.descriptorUuidString, true, command)
    }

    override fun disableCharacteristicIndicate(command: DisableCharacteristicIndicateCommand) {
        super.disableCharacteristicIndicate(command)
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
            command.failureAndComplete("特征值不存在：${characteristicUuidString}")
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
            command.failureAndComplete("特征值不存在：${characteristicUuidString}")
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

    override fun close(command: CloseCommand) {
        super.close(command)
        mWriteJob?.cancel()
        mDelayJob?.cancel()
        mBluetoothGatt?.disconnect()
        mBluetoothGatt?.close()
        mBluetoothGatt = null
        val curCommand = mCurCommand
        if (curCommand != null && curCommand !is DisconnectCommand) {
            curCommand.failureAndComplete("主动断开连接")
        }
        mCurCommand = null
        command.successAndComplete()
    }

}