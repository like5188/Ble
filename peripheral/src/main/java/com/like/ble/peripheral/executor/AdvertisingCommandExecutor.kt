package com.like.ble.peripheral.executor

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import androidx.activity.ComponentActivity
import com.like.ble.peripheral.command.StartAdvertisingCommand
import com.like.ble.peripheral.command.StopAdvertisingCommand
import com.like.ble.util.BleBroadcastReceiverManager
import com.like.ble.util.getBluetoothAdapter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙广播相关的命令执行者
 * 可以进行发送广播、停止广播操作
 *
 * 外围设备会设定一个广播间隔，每个广播间隔中，都会发送自己的广播数据。广播间隔越长，越省电。一个没有被连接的Ble外设会不断发送广播数据，这时可以被多个中心设备发现。一旦外设被连接，则会马上停止广播。
 * GAP 中外围设备通过两种方式向外广播数据：广播数据 和 扫描回复。
 * 每种数据最长可以包含 31 byte。
 * 广播数据是必需的，因为外设必需不停的向外广播，让中心设备知道它的存在。</br>
 * 扫描回复是可选的，中心设备可以向外设请求扫描回复，这里包含一些设备额外的信息。
 */
@SuppressLint("MissingPermission")
class AdvertisingCommandExecutor(private val mActivity: ComponentActivity) : PeripheralCommandExecutor() {
    private val mIsSending = AtomicBoolean(false)
    private var mStartAdvertisingCommand: StartAdvertisingCommand? = null
    private val mBleBroadcastReceiverManager: BleBroadcastReceiverManager by lazy {
        BleBroadcastReceiverManager(mActivity,
            onBleOff = {
                if (mIsSending.compareAndSet(true, false)) {
                    mStartAdvertisingCommand?.error("蓝牙被关闭，广播停止了")
                }
            }
        )
    }
    private val mAdvertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            val errorMsg = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes."
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Failed to start advertising because no advertising instance is available."
                ADVERTISE_FAILED_ALREADY_STARTED -> "Failed to start advertising as the advertising is already started"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Operation failed due to an internal error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "This feature is not supported on this platform"
                else -> "errorCode=$errorCode"
            }
            mStartAdvertisingCommand?.error(errorMsg)
            mIsSending.set(false)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            mStartAdvertisingCommand?.complete()
        }
    }

    init {
        mBleBroadcastReceiverManager.register()
    }

    @Synchronized
    override fun startAdvertising(command: StartAdvertisingCommand) {
        if (mIsSending.compareAndSet(false, true)) {
            val bluetoothLeAdvertiser = mActivity.getBluetoothAdapter()?.bluetoothLeAdvertiser
            if (bluetoothLeAdvertiser == null) {
                command.error("phone does not support Bluetooth Advertiser")
                return
            }

            // 设置设备名字
            if (command.deviceName.isNotEmpty()) {
                mActivity.getBluetoothAdapter()?.name = command.deviceName
            }

            mStartAdvertisingCommand = command
            bluetoothLeAdvertiser.startAdvertising(
                command.settings,
                command.advertiseData,
                command.scanResponse,
                mAdvertiseCallback
            )
        } else {
            command.error("正在广播中")
        }
    }

    @Synchronized
    override fun stopAdvertising(command: StopAdvertisingCommand) {
        if (mIsSending.compareAndSet(true, false)) {
            mActivity.getBluetoothAdapter()?.bluetoothLeAdvertiser?.stopAdvertising(mAdvertiseCallback)
            mStartAdvertisingCommand?.error("广播停止了")
            command.complete()
        } else {
            mStartAdvertisingCommand?.error("广播未开启")
            command.error("广播未开启")
        }
    }

    @Synchronized
    override fun close() {
        stopAdvertising(StopAdvertisingCommand())
        mStartAdvertisingCommand = null
        mBleBroadcastReceiverManager.unregister()
    }

}