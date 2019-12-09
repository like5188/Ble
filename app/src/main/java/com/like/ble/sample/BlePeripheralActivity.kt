package com.like.ble.sample

import android.bluetooth.*
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.like.ble.BleManager
import com.like.ble.sample.databinding.ActivityBlePeripheralBinding

/**
 * 蓝牙外围设备
 * 自安卓5.0后，谷歌加入了对安卓手机作为低功耗蓝牙外围设备，即服务端的支持。使得手机可以通过低功耗蓝牙进行相互通信。
 * 实现这一功能其实只需要分为设置广播和设置服务器两个部分完成即可
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BlePeripheralActivity : AppCompatActivity() {
    companion object {
        private const val UUID_SERVICE: String = "0000fff0-0000-1000-8000-00805f9b34fb"
        private const val UUID_CHARACTERISTIC_READ: String = "0000fff1-0000-1000-8000-00805f9b34fb"
        private const val UUID_CHARACTERISTIC_WRITE: String = "0000fff2-0000-1000-8000-00805f9b34fb"
        private const val UUID_DESCRIPTOR: String = "00002902-0000-1000-8000-00805f9b34fb"
    }

    private val mBinding: ActivityBlePeripheralBinding by lazy {
        DataBindingUtil.setContentView<ActivityBlePeripheralBinding>(
            this,
            R.layout.activity_ble_peripheral
        )
    }
    private val mBleManager: BleManager by lazy {
        BleManager(this)
    }

    private val bluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        private var mCurWriteData: ByteArray? = null

        /**
         * @param newState  连接状态，只能为BluetoothProfile.STATE_CONNECTED和BluetoothProfile.STATE_DISCONNECTED。
         */
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            appendText("onConnectionStateChange device=$device status=$status newState=$newState")
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            appendText("onServiceAdded status=$status service=${service.uuid}")
        }

        /**
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
            if (curWriteData != null && curWriteData.isNotEmpty() && curWriteData[0] == 0x1.toByte()) {
                // 此方法要求作出响应
                mBleManager.getBluetoothGattServer()?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    byteArrayOf(0x07, 0x08)
                )// 最后一个参数是传的数据。
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
                mBleManager.getBluetoothGattServer()?.sendResponse(
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
            mBleManager.getBluetoothGattServer()?.sendResponse(
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
            mBleManager.getBluetoothGattServer()?.sendResponse(
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
        mBleManager.getLiveData().observe(this, androidx.lifecycle.Observer {
            appendText(it?.status?.des ?: "")
        })
    }

    fun init(view: View) {
        mBleManager.initBle()
    }

    fun startAdvertising(view: View) {
        mBleManager.startAdvertising(
            UUID_SERVICE, UUID_CHARACTERISTIC_READ,
            UUID_CHARACTERISTIC_WRITE, UUID_DESCRIPTOR, bluetoothGattServerCallback
        )
    }

    fun stopAdvertising(view: View) {
        mBleManager.stopAdvertising()
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
        mBleManager.close()
        super.onDestroy()
    }

    fun clearLog(view: View) {
        mBinding.tvStatus.text = ""
    }
}
