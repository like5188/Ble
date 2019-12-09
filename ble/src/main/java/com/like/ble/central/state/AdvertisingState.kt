package com.like.ble.central.state

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.central.model.BleResult
import com.like.ble.central.model.BleStatus
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙广播状态
 * 可以进行扫描操作
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AdvertisingState(
    private val mActivity: FragmentActivity,
    private val mBleResultLiveData: MutableLiveData<BleResult>,
    private var mBluetoothManager: BluetoothManager?,
    private var mBluetoothAdapter: BluetoothAdapter?
) : BaseBleState() {
    private val mIsRunning = AtomicBoolean(false)
    private var mBluetoothGattServer: BluetoothGattServer? = null
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null

    private val mAdvertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            val errorMsg = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes."
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Failed to start advertising because no advertising instance is available."
                ADVERTISE_FAILED_ALREADY_STARTED -> "Failed to start advertising as the advertising is already started"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Operation failed due to an internal error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "This feature is not supported on this platform"
                else -> "errorCode=$errorCode"
            }
            mBleResultLiveData.postValue(
                BleResult(
                    BleStatus.START_ADVERTISING_FAILURE,
                    errorMsg = errorMsg
                )
            )
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            mBleResultLiveData.postValue(BleResult(BleStatus.START_ADVERTISING_SUCCESS))
            initServices()//该方法是添加一个服务，在此处调用即将服务广播出去
        }
    }

    private var mServiceUuid: UUID? = null
    private var mReadCharUuid: UUID? = null
    private var mWriteCharUuid: UUID? = null
    private var mDescriptorUuid: UUID? = null
    private var mBluetoothGattServerCallback: BluetoothGattServerCallback? = null

    override fun startAdvertising(
        serviceUuidString: String,
        readCharUuidString: String,
        writeCharUuidString: String,
        descriptorUuidString: String,
        bluetoothGattServerCallback: BluetoothGattServerCallback
    ) {
        if (mIsRunning.compareAndSet(false, true)) {
            mBleResultLiveData.postValue(BleResult(BleStatus.START_ADVERTISING))
            if (mBluetoothLeAdvertiser == null) {
                mBluetoothLeAdvertiser = mBluetoothAdapter?.bluetoothLeAdvertiser
                if (mBluetoothLeAdvertiser == null) {
                    mBleResultLiveData.postValue(
                        BleResult(
                            BleStatus.START_ADVERTISING_FAILURE,
                            errorMsg = "phone does not support Bluetooth Advertiser"
                        )
                    )
                    return
                }
            }

            try {
                mServiceUuid = UUID.fromString(serviceUuidString)
                mReadCharUuid = UUID.fromString(readCharUuidString)
                mWriteCharUuid = UUID.fromString(writeCharUuidString)
                mDescriptorUuid = UUID.fromString(descriptorUuidString)
                mBluetoothGattServerCallback = bluetoothGattServerCallback
            } catch (e: Exception) {
                mBleResultLiveData.postValue(
                    BleResult(
                        BleStatus.START_ADVERTISING_FAILURE,
                        errorMsg = "args is error"
                    )
                )
                return
            }

            mBluetoothLeAdvertiser?.startAdvertising(
                createAdvertiseSettings(),
                createAdvertiseData(byteArrayOf(0x34, 0x56)),
                createScanResponseAdvertiseData(),
                mAdvertiseCallback
            )
        }
    }

    override fun stopAdvertising() {
        if (mIsRunning.compareAndSet(true, false)) {
            mBleResultLiveData.postValue(BleResult(BleStatus.STOP_ADVERTISING))
            mBluetoothLeAdvertiser?.stopAdvertising(mAdvertiseCallback)
        }
    }

    override fun close() {
        stopAdvertising()
        mServiceUuid = null
        mReadCharUuid = null
        mWriteCharUuid = null
        mDescriptorUuid = null
        mBluetoothGattServerCallback = null
        mBluetoothGattServer = null
        mBluetoothLeAdvertiser = null
        mBluetoothAdapter = null
        mBluetoothManager = null
    }

    override fun getBluetoothAdapter(): BluetoothAdapter? {
        return mBluetoothAdapter
    }

    override fun getBluetoothManager(): BluetoothManager? {
        return mBluetoothManager
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
            .addServiceData(ParcelUuid(mServiceUuid), byteArrayOf(1, 2, 3, 4, 5))
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
        val bluetoothGattServer =
            mBluetoothManager?.openGattServer(mActivity, mBluetoothGattServerCallback) ?: return
        val service = BluetoothGattService(mServiceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val characteristicRead = BluetoothGattCharacteristic(
            mReadCharUuid,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        val descriptor = BluetoothGattDescriptor(
            mDescriptorUuid,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        characteristicRead.addDescriptor(descriptor)
        service.addCharacteristic(characteristicRead)

        val characteristicWrite = BluetoothGattCharacteristic(
            mWriteCharUuid,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicWrite)

        bluetoothGattServer.addService(service)
        mBluetoothGattServer = bluetoothGattServer
    }

    override fun getBluetoothGattServer(): BluetoothGattServer? {
        return mBluetoothGattServer
    }

}