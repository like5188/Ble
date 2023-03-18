package com.like.ble.peripheral.executor

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import androidx.activity.ComponentActivity
import com.like.ble.callback.BleCallback
import com.like.ble.exception.BleException
import com.like.ble.peripheral.callback.AdvertisingCallbackManager
import com.like.ble.util.getBluetoothAdapter
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙广播相关的命令执行者
 * 可以进行发送广播、停止广播操作
 * 注意：每次发送广播，都有可能会导致mac地址发生改变。
 *
 * 外围设备会设定一个广播间隔，每个广播间隔中，都会发送自己的广播数据。广播间隔越长，越省电。一个没有被连接的Ble外设会不断发送广播数据，这时可以被多个中心设备发现。一旦外设被连接，则会马上停止广播。
 * GATT 连接是独占的。也就是一个 BLE 外设同时只能被一个中心设备连接。一旦外设被连接，它就会马上停止广播，这样它就对其他设备不可见了。当设备断开，它又开始广播。中心设备和外设需要双向通信的话，唯一的方式就是建立 GATT 连接。
 * GAP 中外围设备通过两种方式向外广播数据：广播数据 和 扫描回复。
 * 每种数据最长可以包含 31 byte。
 * 广播数据是必需的，因为外设必需不停的向外广播，让中心设备知道它的存在。</br>
 * 扫描回复是可选的，中心设备可以向外设请求扫描回复，这里包含一些设备额外的信息。
 */
@SuppressLint("MissingPermission")
class AdvertisingExecutor(activity: ComponentActivity) : BaseAdvertisingExecutor(activity) {
    private val advertisingCallbackManager: AdvertisingCallbackManager by lazy {
        AdvertisingCallbackManager()
    }

    override fun onStartAdvertising(
        continuation: CancellableContinuation<Unit>,
        settings: AdvertiseSettings,
        advertiseData: AdvertiseData,
        scanResponse: AdvertiseData?,
        deviceName: String
    ) {
        val bluetoothLeAdvertiser = activity.getBluetoothAdapter()?.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            continuation.resumeWithException(BleException("phone does not support Bluetooth Advertiser"))
        }
        // 设置设备名字
        if (deviceName.isNotEmpty()) {
            activity.getBluetoothAdapter()?.name = deviceName
        }
        advertisingCallbackManager.setAdvertisingCallback(object : BleCallback() {
            override fun onSuccess() {
                continuation.resume(Unit)
            }

            override fun onError(exception: BleException) {
                continuation.resumeWithException(exception)
            }
        })
        bluetoothLeAdvertiser?.startAdvertising(
            settings,
            advertiseData,
            scanResponse,
            advertisingCallbackManager.getAdvertiseCallback()
        )
    }

    override fun onStopAdvertising() {
        activity.getBluetoothAdapter()?.bluetoothLeAdvertiser?.stopAdvertising(advertisingCallbackManager.getAdvertiseCallback())
    }

}
