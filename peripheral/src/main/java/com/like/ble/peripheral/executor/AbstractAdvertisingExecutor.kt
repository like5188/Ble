package com.like.ble.peripheral.executor

import android.Manifest
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.executor.BleExecutor

/**
 * 外围设备广播执行者
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
abstract class AbstractAdvertisingExecutor(activity: ComponentActivity) : BleExecutor(
    activity,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 中的新蓝牙权限
        // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        emptyArray()
    }
) {

    /**
     * 开始广播
     * 由于发送广播操作不涉及到和其它设备进行交互，所以执行很快，不需要用 flow 来提供各种中间状态的结果。
     *
     * @param settings          广播的设置
     * @param advertiseData     广播的数据
     * @param scanResponse      与广播数据相关联的扫描响应数据
     * @param deviceName        设备名称。默认为设备名称。
     */
    abstract suspend fun startAdvertising(
        settings: AdvertiseSettings,
        advertiseData: AdvertiseData,
        scanResponse: AdvertiseData? = null,
        deviceName: String = "",
        timeout: Long = 10000L,
    )

    /**
     * 停止广播
     */
    abstract fun stopAdvertising()

}
