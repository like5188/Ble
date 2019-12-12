package com.like.ble.sample

import android.bluetooth.*
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.text.method.ScrollingMovementMethod
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.like.ble.BleManager
import com.like.ble.command.concrete.InitCommand
import com.like.ble.command.concrete.StartAdvertisingCommand
import com.like.ble.command.concrete.StopAdvertisingCommand
import com.like.ble.sample.databinding.ActivityBlePeripheralBinding
import com.like.ble.utils.getBluetoothManager
import java.util.*

/**
 * 蓝牙外围设备
 * 自安卓5.0后，谷歌加入了对安卓手机作为低功耗蓝牙外围设备，即服务端的支持。使得手机可以通过低功耗蓝牙进行相互通信。
 * 实现这一功能其实只需要分为设置广播和设置服务器两个部分完成即可
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BlePeripheralActivity : AppCompatActivity() {
    companion object {
        private val UUID_SERVICE: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_READ: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_WRITE: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
        private val UUID_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val mBinding: ActivityBlePeripheralBinding by lazy {
        DataBindingUtil.setContentView<ActivityBlePeripheralBinding>(this, R.layout.activity_ble_peripheral)
    }
    private val mBleManager: BleManager by lazy {
        BleManager(this)
    }
    private var mBluetoothGattServer: BluetoothGattServer? = null
    private val mBluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        private var mCurWriteData: ByteArray? = null

        /**
         * @param newState  连接状态，只能为[BluetoothProfile.STATE_CONNECTED]和[BluetoothProfile.STATE_DISCONNECTED]。
         */
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            appendText("onConnectionStateChange device=$device status=$status newState=$newState")
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            appendText("onServiceAdded status=$status service=${service.uuid}")
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
            appendText("onCharacteristicReadRequest device=$device requestId=$requestId offset=$offset characteristic=$characteristic value=${characteristic.value?.contentToString()}")
            val curWriteData = mCurWriteData
            if (curWriteData == null || curWriteData.isEmpty()) {
                // 此方法要求作出响应
                mBluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    byteArrayOf()// 最后一个参数是传的数据。
                )
            } else {
                when (curWriteData[0]) {
                    0x1.toByte() -> {
                        mBluetoothGattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            offset,
                            byteArrayOf(0x02)
                        )
                    }
                    0x2.toByte() -> {
                        mBluetoothGattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            offset,
                            byteArrayOf(0x03)
                        )
                    }
                }
            }
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
            appendText("onCharacteristicWriteRequest device=$device requestId=$requestId characteristic=$characteristic preparedWrite=$preparedWrite responseNeeded=$responseNeeded offset=$offset value=${value.contentToString()}")
            mCurWriteData = value
            // 如果 responseNeeded=true（此属性由中心设备的 characteristic.setWriteType() 方法设置），则必须调用 sendResponse()方法回复中心设备，这个方法会触发中心设备的 BluetoothGattCallback.onCharacteristicWrite() 方法，然后中心设备才能继续下次写数据，否则不能再次写入数据。
            // 如果 responseNeeded=false，那么不需要 sendResponse() 方法，也会触发中心设备的 BluetoothGattCallback.onCharacteristicWrite() 方法
            if (responseNeeded) {
                mBluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    byteArrayOf(0x03, 0x04)
                )
            }
            // 外围设备向中心设备不能发送数据，必须通过notify 或者indicate的方式，andorid只发现notify接口。
            // 调用 notifyCharacteristicChanged() 方法向中心设备发送数据，会触发 onNotificationSent() 方法和中心设备的 BluetoothGattCallback.onCharacteristicChanged() 方法。
//            characteristic.value = byteArrayOf(0x05, 0x06)
//            mBluetoothGattServer?.notifyCharacteristicChanged(device, characteristic, false)// 最后一个参数表示是否需要客户端确认
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            appendText("onDescriptorReadRequest device=$device requestId=$requestId offset=$offset descriptor=$descriptor")
            mBluetoothGattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                byteArrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
            )
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
            appendText("onDescriptorWriteRequest device=$device requestId=$requestId descriptor=$descriptor preparedWrite=$preparedWrite responseNeeded=$responseNeeded offset=$offset value=${value.contentToString()}")
            mBluetoothGattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                byteArrayOf(19, 18, 17, 16, 15, 14, 13, 12, 11, 10)
            )
        }

        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
            appendText("onExecuteWrite device=$device requestId=$requestId execute=$execute")
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            appendText("onNotificationSent device=$device status=$status")
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            appendText("onMtuChanged device=$device mtu=$mtu")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding.tvStatus.movementMethod = ScrollingMovementMethod()
    }

    fun init(view: View) {
        mBleManager.sendCommand(
            InitCommand({
                appendText("初始化成功")
            }, {
                appendText("初始化失败")
            })
        )
    }

    fun startAdvertising(view: View) {
        mBleManager.sendCommand(
            StartAdvertisingCommand(
                createAdvertiseSettings(),
                createAdvertiseData(byteArrayOf(0x34, 0x56)),
                createScanResponseAdvertiseData(),
                {
                    appendText("广播成功")
                    initServices()//该方法是添加一个服务，在此处调用即将服务广播出去
                },
                {
                    appendText("广播失败")
                }
            )
        )
    }

    fun stopAdvertising(view: View) {
        appendText("停止广播")
        mBleManager.sendCommand(StopAdvertisingCommand())
    }

    private fun appendText(text: String) {
        runOnUiThread {
            val sb = StringBuilder(mBinding.tvStatus.text)
            sb.append(text).append("\n\n")
            mBinding.tvStatus.text = sb.toString()
            val offset = mBinding.tvStatus.lineCount * mBinding.tvStatus.lineHeight
            mBinding.tvStatus.scrollTo(
                0,
                offset - mBinding.tvStatus.height + mBinding.tvStatus.lineHeight
            )
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

    fun clearLog(view: View) {
        mBinding.tvStatus.text = ""
    }

    private fun createAdvertiseData(data: ByteArray): AdvertiseData {
        return AdvertiseData.Builder()
            .addManufacturerData(0x01AC, data)
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(true)
            .build()
    }

    private fun createScanResponseAdvertiseData(): AdvertiseData {
        return AdvertiseData.Builder()
            .addServiceData(ParcelUuid(UUID_SERVICE), byteArrayOf(1, 2, 3, 4, 5))
            .setIncludeTxPowerLevel(true)
            .build()
    }

    private fun createAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            // 设置广播的模式，低功耗，平衡和低延迟三种模式；
            // 对应  AdvertiseSettings.ADVERTISE_MODE_LOW_POWER  ,ADVERTISE_MODE_BALANCED ,ADVERTISE_MODE_LOW_LATENCY
            // 从左右到右，广播的间隔会越来越短
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            // 设置是否可以连接。
            // 广播分为可连接广播和不可连接广播，一般不可连接广播应用在iBeacon设备上，这样APP无法连接上iBeacon设备
            .setConnectable(true)
            // 设置广播的最长时间，最大值为常量AdvertiseSettings.LIMITED_ADVERTISING_MAX_MILLIS = 180 * 1000;  180秒
            // 设为0时，代表无时间限制会一直广播
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

    private fun initServices() {
        if (mBluetoothGattServer != null) return
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothGattServer = bluetoothManager?.openGattServer(this, mBluetoothGattServerCallback) ?: return
        val service = BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val characteristicRead = BluetoothGattCharacteristic(
            UUID_CHARACTERISTIC_READ,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        val descriptor = BluetoothGattDescriptor(
            UUID_DESCRIPTOR,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        characteristicRead.addDescriptor(descriptor)
        service.addCharacteristic(characteristicRead)

        val characteristicWrite = BluetoothGattCharacteristic(
            UUID_CHARACTERISTIC_WRITE,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicWrite)

        bluetoothGattServer.addService(service)
        mBluetoothGattServer = bluetoothGattServer
    }
}
