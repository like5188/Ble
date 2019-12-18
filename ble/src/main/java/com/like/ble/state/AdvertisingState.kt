package com.like.ble.state

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.Build
import androidx.annotation.RequiresApi
import com.like.ble.command.CloseCommand
import com.like.ble.command.StartAdvertisingCommand
import com.like.ble.command.StopAdvertisingCommand
import com.like.ble.utils.getBluetoothAdapter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙广播状态
 * 可以进行发送广播、停止广播操作
 *
 * 外围设备会设定一个广播间隔，每个广播间隔中，都会发送自己的广播数据。广播间隔越长，越省电。一个没有被连接的Ble外设会不断发送广播数据，这时可以被多个中心设备发现。一旦外设被连接，则会马上停止广播。
 * GAP 中外围设备通过两种方式向外广播数据：广播数据 和 扫描回复。
 * 每种数据最长可以包含 31 byte。
 * 广播数据是必需的，因为外设必需不停的向外广播，让中心设备知道它的存在。</br>
 * 扫描回复是可选的，中心设备可以向外设请求扫描回复，这里包含一些设备额外的信息。
 */
class AdvertisingState : State() {
    private val mIsRunning = AtomicBoolean(false)
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private val mAdvertiseCallback: AdvertiseCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            val curCommand = mCurCommand
            if (curCommand is StartAdvertisingCommand) {
                // 扫描太频繁会返回ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED，表示app无法注册，无法开始扫描。
                val errorMsg = when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes."
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Failed to start advertising because no advertising instance is available."
                    ADVERTISE_FAILED_ALREADY_STARTED -> "Failed to start advertising as the advertising is already started"
                    ADVERTISE_FAILED_INTERNAL_ERROR -> "Operation failed due to an internal error"
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "This feature is not supported on this platform"
                    else -> "errorCode=$errorCode"
                }
                curCommand.failureAndComplete(errorMsg)
                mIsRunning.set(false)
            }
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            val curCommand = mCurCommand
            if (curCommand is StartAdvertisingCommand) {
                curCommand.successAndComplete()
            }
        }
    }

    @Synchronized
    override fun startAdvertising(command: StartAdvertisingCommand) {
        mCurCommand = command
        if (mIsRunning.compareAndSet(false, true)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                command.failureAndComplete("phone does not support Bluetooth Advertiser")
                return
            }
            if (mBluetoothLeAdvertiser == null) {
                mBluetoothLeAdvertiser = mActivity.getBluetoothAdapter()?.bluetoothLeAdvertiser
                if (mBluetoothLeAdvertiser == null) {
                    command.failureAndComplete("phone does not support Bluetooth Advertiser")
                    return
                }
            }

            // 设置设备名字
            if (command.deviceName.isNotEmpty()) {
                mActivity.getBluetoothAdapter()?.name = command.deviceName
            }

            mBluetoothLeAdvertiser?.startAdvertising(command.settings, command.advertiseData, command.scanResponse, mAdvertiseCallback)
        } else {
            command.failureAndComplete("正在广播中")
        }
    }

    @Synchronized
    override fun stopAdvertising(command: StopAdvertisingCommand) {
        if (mIsRunning.compareAndSet(true, false)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                command.failureAndComplete("phone does not support Bluetooth Advertiser")
                return
            }

            val curCommand = mCurCommand
            if (curCommand is StartAdvertisingCommand) {
                curCommand.failureAndComplete("主动关闭了广播")
            }

            mCurCommand = command

            mBluetoothLeAdvertiser?.stopAdvertising(mAdvertiseCallback)
            command.successAndComplete()
        } else {
            command.failureAndComplete("广播已经停止")
        }
    }

    @Synchronized
    override fun close(command: CloseCommand) {
        stopAdvertising(StopAdvertisingCommand())
        mBluetoothLeAdvertiser = null
        mCurCommand = null
        command.successAndComplete()
    }

}