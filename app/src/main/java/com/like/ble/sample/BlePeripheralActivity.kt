package com.like.ble.sample

import android.bluetooth.*
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.like.ble.IBleManager
import com.like.ble.PeripheralManager
import com.like.ble.command.StartAdvertisingCommand
import com.like.ble.command.StopAdvertisingCommand
import com.like.ble.command.base.Command
import com.like.ble.sample.databinding.ActivityBlePeripheralBinding
import com.like.ble.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 蓝牙外围设备
 * 自安卓5.0后，谷歌加入了对安卓手机作为低功耗蓝牙外围设备，即服务端的支持。使得手机可以通过低功耗蓝牙进行相互通信。
 * 实现这一功能其实只需要分为设置广播和设置服务器两个部分完成即可
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BlePeripheralActivity : AppCompatActivity() {
    companion object {
        // 0000????-0000-1000-8000-00805f9b34fb ????就表示4个可以自定义16进制数
        private val UUID_SERVICE_1: UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
        private val UUID_SERVICE_2: UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")
        private val UUID_SERVICE_3: UUID = UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_1: UUID = UUID.fromString("0000ff11-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_2: UUID = UUID.fromString("0000ff12-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_3: UUID = UUID.fromString("0000ff13-0000-1000-8000-00805f9b34fb")
        private val UUID_DESCRIPTOR_1: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")// 使能对应《Characteristic》的notification或Indication
        private val UUID_DESCRIPTOR_2: UUID = UUID.fromString("0000ff21-0000-1000-8000-00805f9b34fb")
        private val UUID_DESCRIPTOR_3: UUID = UUID.fromString("0000ff22-0000-1000-8000-00805f9b34fb")
    }

    private val mBinding: ActivityBlePeripheralBinding by lazy {
        DataBindingUtil.setContentView<ActivityBlePeripheralBinding>(this, R.layout.activity_ble_peripheral)
    }
    private val mBleManager: IBleManager by lazy { PeripheralManager(this) }
    private var mBluetoothGattServer: BluetoothGattServer? = null
    private val mBluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        private val mResponseData: ByteArray by lazy {
            val arr = ByteArray(600)// 最大只能传输600字节
            for (i in 1 until arr.size) {
                arr[i - 1] = i.toByte()
            }
            arr[arr.size - 1] = Byte.MAX_VALUE// 一帧结束的标志
            arr
        }
        private var mMtu = 23

        /**
         * @param newState  连接状态，只能为[BluetoothProfile.STATE_CONNECTED]和[BluetoothProfile.STATE_DISCONNECTED]。
         */
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            appendText("--> onConnectionStateChange", false, R.color.ble_text_blue)
            appendText("device=${device.address} status=${getConnectionStateString(status)} newState=${getConnectionStateString(newState)}")
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            appendText("--> onServiceAdded", false, R.color.ble_text_blue)
            appendText("status=${getBluetoothGattStatusString(status)}", false)
            appendText("service：${service.uuid.getValidString()}", false)
            appendText("type=${service.getTypeString()}", false, R.color.ble_text_black_2)
            service.characteristics.forEach { characteristic ->
                appendText("characteristic：${characteristic.uuid.getValidString()}", false)
                appendText("Properties=${characteristic.getPropertiesString()}", false, R.color.ble_text_black_2)
                characteristic.descriptors.forEach { descriptor ->
                    appendText("descriptor：${descriptor.uuid.getValidString()}", false)
                }
            }
            appendText("", false)
        }

        /**
         * 此方法要求作出响应
         *
         * @param requestId     请求的标识
         * @param offset        特性值偏移量
         */
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            appendText("--> onCharacteristicReadRequest", false, R.color.ble_text_blue)
            appendText(
                "device=${device.address} requestId=$requestId offset=$offset characteristic=${characteristic.uuid.getValidString()} value=${characteristic.value?.contentToString()}",
                false
            )

            val response = when (characteristic.uuid) {
                UUID_CHARACTERISTIC_1 -> {
                    mResponseData.copyOfRangeByLength(offset, mMtu - 1)
                }
                else -> {
                    byteArrayOf(Byte.MAX_VALUE)
                }
            }
            appendText("sendResponse：size=${response.size} ${response.contentToString()}")
            // 注意：如果所传数据长度>=offset的步进（MTU - 1），就会自动再次触发onCharacteristicReadRequest()方法。
            // 传输的数据最大为600子节
            // sendResponse()中的参数offset可以随便传，不会影响onCharacteristicReadRequest()方法返回的offset。
            mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response)
        }

        /**
         * @param preparedWrite     true则写操作必须排队等待稍后执行
         * @param responseNeeded    是否需要响应，需要响应则必须调用 sendResponse()
         */
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            appendText("--> onCharacteristicWriteRequest", false, R.color.ble_text_blue)
            appendText("device=${device.address} requestId=$requestId characteristic=${characteristic.uuid.getValidString()} preparedWrite=$preparedWrite responseNeeded=$responseNeeded offset=$offset value=${value.contentToString()}")
            // 如果 responseNeeded=true（此属性由中心设备的 characteristic.setWriteType() 方法设置），则必须调用 sendResponse()方法回复中心设备，这个方法会触发中心设备的 BluetoothGattCallback.onCharacteristicWrite() 方法，然后中心设备才能继续下次写数据，否则不能再次写入数据。
            // 如果 responseNeeded=false，那么不需要 sendResponse() 方法，也会触发中心设备的 BluetoothGattCallback.onCharacteristicWrite() 方法
            if (responseNeeded) {
                mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, byteArrayOf())
            }

            when (value[0]) {
                0x1.toByte() -> {
                    // 外围设备向中心设备不能发送数据，必须通过notify 或者indicate的方式，andorid只发现notify接口。
                    // 调用 notifyCharacteristicChanged() 方法向中心设备发送数据，会触发 onNotificationSent() 方法和中心设备的 BluetoothGattCallback.onCharacteristicChanged() 方法。
                    // 注意：默认mtu下一次只能传递20字节。
                    mResponseData.batch(mMtu - 3).forEach {
                        characteristic.value = it
                        mBluetoothGattServer?.notifyCharacteristicChanged(device, characteristic, false)// 最后一个参数表示是否需要客户端确认
                    }
                }
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            appendText("--> onDescriptorReadRequest", false, R.color.ble_text_blue)
            appendText("device=${device.address} requestId=$requestId offset=$offset descriptor=${descriptor.uuid.getValidString()}", false)

            val response = when (descriptor.uuid) {
                UUID_DESCRIPTOR_1 -> {
                    byteArrayOf(Byte.MAX_VALUE, Byte.MAX_VALUE)
                }
                else -> {
                    mResponseData.copyOfRangeByLength(offset, mMtu - 1)
                }
            }
            appendText("sendResponse：size=${response.size} ${response.contentToString()}")

            mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            appendText("--> onDescriptorWriteRequest", false, R.color.ble_text_blue)
            appendText("device=${device.address} requestId=$requestId descriptor=${descriptor.uuid.getValidString()} preparedWrite=$preparedWrite responseNeeded=$responseNeeded offset=$offset value=${value.contentToString()}")
            if (responseNeeded) {
                mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
            appendText("--> onExecuteWrite", false, R.color.ble_text_blue)
            appendText("device=${device.address} requestId=$requestId execute=$execute")
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            appendText("--> onNotificationSent", false, R.color.ble_text_blue)
            appendText("device=${device.address} status=${getBluetoothGattStatusString(status)}")
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            appendText("--> onMtuChanged", false, R.color.ble_text_blue)
            appendText("device=${device.address} mtu=$mtu")
            mMtu = mtu
        }

        override fun onPhyRead(device: BluetoothDevice, txPhy: Int, rxPhy: Int, status: Int) {
            appendText("--> onPhyRead", false, R.color.ble_text_blue)
            appendText("device=${device.address} txPhy=$txPhy rxPhy=$rxPhy status=${getBluetoothGattStatusString(status)}")
        }

        override fun onPhyUpdate(device: BluetoothDevice, txPhy: Int, rxPhy: Int, status: Int) {
            appendText("--> onPhyUpdate", false, R.color.ble_text_blue)
            appendText("device=${device.address} txPhy=$txPhy rxPhy=$rxPhy status=${getBluetoothGattStatusString(status)}")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding.tvStatus.movementMethod = ScrollingMovementMethod()
    }

    fun startAdvertising(view: View) {
        mBleManager.sendCommand(
            StartAdvertisingCommand(
                createAdvertiseSettings(),
                createAdvertiseData(),
                createScanResponseAdvertiseData(byteArrayOf(0x34, 0x56)),// 外设必须广播广播包，扫描包是可选。但添加扫描包也意味着广播更多得数据，即可广播62个字节。
                "BLE测试设备",
                object : Command.Callback() {
                    override fun onCompleted() {
                        mBinding.tvAdvertisingStatus.setTextColor(ContextCompat.getColor(this@BlePeripheralActivity, R.color.ble_text_blue))
                        mBinding.tvAdvertisingStatus.text = "广播已开启"
                        initServices()//该方法是添加一个服务，在此处调用即将服务广播出去
                    }

                    override fun onFailure(t: Throwable) {
                        mBinding.tvAdvertisingStatus.setTextColor(ContextCompat.getColor(this@BlePeripheralActivity, R.color.ble_text_red))
                        mBinding.tvAdvertisingStatus.text = t.message ?: "广播停止了"
                        if (!isBluetoothEnable()) {// 说明关闭了蓝牙
                            getBluetoothManager()?.getConnectedDevices(BluetoothProfile.GATT)?.forEach { device ->
                                mBluetoothGattServer?.cancelConnection(device)
                            }
                            mBluetoothGattServer?.clearServices()
                            mBluetoothGattServer?.close()
                            mBluetoothGattServer = null
                        }
                    }
                }
            )
        )
    }

    fun stopAdvertising(view: View) {
        mBleManager.sendCommand(StopAdvertisingCommand())
    }

    @Synchronized
    private fun appendText(text: String, isBreak: Boolean = true, @ColorRes textColorResId: Int = R.color.ble_text_black_1) {
        runOnUiThread {
            val textColor = ContextCompat.getColor(this, textColorResId)
            val textColorHexString = Integer.toHexString(textColor).substring(2)
            val htmlText = String.format("""<font color="#$textColorHexString">$text</font>""")
            mBinding.tvStatus.append(Html.fromHtml(htmlText))
            if (isBreak) {
                mBinding.tvStatus.append("\n\n")
            } else {
                mBinding.tvStatus.append("\n")
            }
            val offset = mBinding.tvStatus.lineCount * mBinding.tvStatus.lineHeight
            if (offset > mBinding.tvStatus.height) {
                mBinding.tvStatus.scrollTo(0, offset - mBinding.tvStatus.height)
            }
        }
    }

    fun clearLog(view: View) {
        mBinding.tvStatus.text = ""
    }

    private fun createAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            // 设置广播的模式，低功耗，平衡和低延迟三种模式；
            // 对应  AdvertiseSettings.ADVERTISE_MODE_LOW_POWER  ,ADVERTISE_MODE_BALANCED ,ADVERTISE_MODE_LOW_LATENCY
            // 从左右到右，广播的间隔会越来越短
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            // 设置是否可以连接。
            // 广播分为可连接广播和不可连接广播，一般不可连接广播应用在iBeacon设备上，这样APP无法连接上iBeacon设备
            .setConnectable(true)
            // 设置广播的最长时间，最大值为常量AdvertiseSettings.LIMITED_ADVERTISING_MAX_MILLIS = 180 * 1000;  180秒
            // 设为0时，代表无时间限制会一直广播，除非调用BluetoothLeAdvertiser#stopAdvertising()。
            .setTimeout(0)
            // 设置广播的信号强度
            // 常量有AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW, ADVERTISE_TX_POWER_LOW, ADVERTISE_TX_POWER_MEDIUM, ADVERTISE_TX_POWER_HIGH
            // 从左到右分别表示强度越来越强.
            // 举例：当设置为ADVERTISE_TX_POWER_ULTRA_LOW时，
            // 手机1和手机2放在一起，手机2扫描到的rssi信号强度为-56左右，
            // 当设置为ADVERTISE_TX_POWER_HIGH  时， 扫描到的信号强度为-33左右，
            // 信号强度越大，表示手机和设备靠的越近
            // ＊ AdvertiseSettings.ADVERTISE_TX_POWER_HIGH -56 dBm @ 1 meter with Nexus 5
            // ＊ AdvertiseSettings.ADVERTISE_TX_POWER_LOW -75 dBm @ 1 meter with Nexus 5
            // ＊ AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM -66 dBm @ 1 meter with Nexus 5
            // ＊ AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW not detected with Nexus 5
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
    }

    /**
     * 广播报文（必须）
     *
     * 一个 AdvertiseData 中数据的长度必须是31个字节，如果不到31个字节 ，则剩下的全用0补全，这部分的数据是无效的，
     * 如果多了会广播失败，错误码如下：
     * [android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE]
     *
     * 这里调用 addServiceUuid(ParcelUuid(UUID_SERVICE))
     * 是为了让使用者调用[android.bluetooth.BluetoothAdapter.startLeScan]能过滤 serviceUuids
     */
    private fun createAdvertiseData(): AdvertiseData {
        return AdvertiseData.Builder()
            .setIncludeDeviceName(true)// 设置广播包中是否包含蓝牙的名称。
            .setIncludeTxPowerLevel(true)// 设置广播包中是否包含蓝牙的发射功率。 数值范围：±127 dBm。
            .addServiceUuid(ParcelUuid(UUID_SERVICE_1))// 添加是为了让使用者扫描到
            .addServiceUuid(ParcelUuid(UUID_SERVICE_2))// 添加是为了让使用者扫描到
            .addServiceUuid(ParcelUuid(UUID_SERVICE_3))// 添加是为了让使用者扫描到
            .build()
    }

    /**
     * 广播扫描响应报文（可选）
     */
    private fun createScanResponseAdvertiseData(data: ByteArray): AdvertiseData {
        return AdvertiseData.Builder()
            // 如果一个外设需要在不连接的情况下对外广播数据，其数据可以存储在UUID对应的数据中，也可以存储在厂商数据中。但由于厂商ID是需要由Bluetooth SIG进行分配的，厂商间一般都将数据设置在厂商数据。
            .addServiceData(ParcelUuid(UUID_SERVICE_1), byteArrayOf(0x01))// 设置特定的UUID和其数据在广播包中
            .addServiceData(ParcelUuid(UUID_SERVICE_2), byteArrayOf(0x02))// 设置特定的UUID和其数据在广播包中
            .addServiceData(ParcelUuid(UUID_SERVICE_3), byteArrayOf(0x03))// 设置特定的UUID和其数据在广播包中
            .addManufacturerData(0xAC, data)// 设置特定厂商Id和其数据在广播包中。前两个字节表示厂商ID,剩下的是厂商自定义的数据。
            .build()
    }

    private fun initServices() {
        if (mBluetoothGattServer != null) return

        lifecycleScope.launch {
            val bluetoothGattServer =
                getBluetoothManager()?.openGattServer(this@BlePeripheralActivity, mBluetoothGattServerCallback) ?: return@launch

            val service1 = BluetoothGattService(UUID_SERVICE_1, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val characteristic1 = BluetoothGattCharacteristic(
                UUID_CHARACTERISTIC_1,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                        BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            val descriptor1 = BluetoothGattDescriptor(
                UUID_DESCRIPTOR_1,
                BluetoothGattDescriptor.PERMISSION_WRITE or
                        BluetoothGattDescriptor.PERMISSION_READ
            )
            characteristic1.addDescriptor(descriptor1)
            service1.addCharacteristic(characteristic1)
            bluetoothGattServer.addService(service1)
            delay(100)

            val service2 = BluetoothGattService(UUID_SERVICE_2, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val characteristic2 = BluetoothGattCharacteristic(
                UUID_CHARACTERISTIC_2,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            val descriptor2 = BluetoothGattDescriptor(
                UUID_DESCRIPTOR_2,
                BluetoothGattDescriptor.PERMISSION_WRITE or
                        BluetoothGattDescriptor.PERMISSION_READ
            )
            characteristic2.addDescriptor(descriptor2)
            service2.addCharacteristic(characteristic2)
            bluetoothGattServer.addService(service2)
            delay(100)

            val service3 = BluetoothGattService(UUID_SERVICE_3, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val characteristic3 = BluetoothGattCharacteristic(
                UUID_CHARACTERISTIC_3,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            val descriptor3 = BluetoothGattDescriptor(
                UUID_DESCRIPTOR_3,
                BluetoothGattDescriptor.PERMISSION_WRITE or
                        BluetoothGattDescriptor.PERMISSION_READ
            )
            characteristic3.addDescriptor(descriptor3)
            service3.addCharacteristic(characteristic3)
            bluetoothGattServer.addService(service3)
            delay(100)

            mBluetoothGattServer = bluetoothGattServer
        }
    }

    override fun onDestroy() {
        getBluetoothManager()?.getConnectedDevices(BluetoothProfile.GATT)?.forEach {
            mBluetoothGattServer?.cancelConnection(it)
        }
        mBluetoothGattServer?.clearServices()
        mBluetoothGattServer?.close()
        mBluetoothGattServer = null
        mBleManager.close()
        super.onDestroy()
    }
}
