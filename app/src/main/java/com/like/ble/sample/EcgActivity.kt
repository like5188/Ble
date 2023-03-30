package com.like.ble.sample

import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.like.ble.central.connect.executor.AbstractConnectExecutor
import com.like.ble.central.connect.executor.ConnectExecutorFactory
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.sample.databinding.ActivityEcgBinding
import com.like.ble.util.createBleUuidBy16Bit
import com.like.ble.util.getValidString
import com.like.common.util.Logger
import com.starcaretech.stardata.StarData
import com.starcaretech.stardata.common.DataReceiverSample
import com.starcaretech.stardata.data.AlertSwitch
import com.starcaretech.stardata.data.DataPoint
import com.starcaretech.stardata.data.DeviceException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 心电
 */
class EcgActivity : AppCompatActivity() {
    private val mBinding: ActivityEcgBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_ecg)
    }
    private val connectExecutor: AbstractConnectExecutor by lazy {
        ConnectExecutorFactory.get(this, "A0:02:19:00:02:19")
    }
    private val serviceUuid = createBleUuidBy16Bit("0001")
    private val commandUuid = createBleUuidBy16Bit("0002")
    private val notificationUuid = createBleUuidBy16Bit("0003")
    private lateinit var commandCharacteristic: BluetoothGattCharacteristic
    private lateinit var notificationCharacteristic: BluetoothGattCharacteristic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding.btnConnect.setOnClickListener {
            connect()
        }
        mBinding.btnDisconnect.setOnClickListener {
            disconnect()
        }
        connectExecutor.requestEnvironment(this)
        lifecycleScope.launch {
            connectExecutor.setNotifyCallback(notificationUuid)
                .catch {
                    Toast.makeText(this@EcgActivity, it.message, Toast.LENGTH_SHORT).show()
                }
                .collectLatest {
                    StarData.putData(it)
                    Logger.d("读取通知(${notificationUuid.getValidString()})传来的数据成功。数据长度：${it.size} ${it.contentToString()}")
                }
        }

        lifecycleScope.launch {
            StarData.setDataReceiver(object : DataReceiverSample() {
                override fun onDeviceException(e: DeviceException) { // 设备异常
                    Logger.e(e)
                }

                override fun onECGData(bytes: ByteArray) {
                    Logger.v(bytes.contentToString())
                }

                override fun onDataPoints(list: List<DataPoint>) {
                    Logger.i(list)
                }

                override fun onAlertSwitch(alertSwitch: AlertSwitch) {
                    // 设备报警开关
                    alertSwitch.setLowPower(3)
                    alertSwitch.setFlash(3)
                    alertSwitch.setLeadOff(3)
                    alertSwitch.setBleStatus(1)
                }
            })
        }
    }

    private fun connect() {
        val preState = mBinding.tvConnectStatus.text
        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_black_1))
        mBinding.tvConnectStatus.text = "连接中……"
        lifecycleScope.launch {
            try {
                val services = connectExecutor.connect()
                val service = services.firstOrNull { it.uuid == serviceUuid } ?: throw BleException("没有找到服务：$serviceUuid")
                val commandCharacteristic = service.getCharacteristic(commandUuid) ?: throw BleException("没有找到命令特征：$commandUuid")
                val notificationCharacteristic =
                    service.getCharacteristic(notificationUuid) ?: throw BleException("没有找到通知特征：$notificationUuid")
                connectExecutor.setCharacteristicNotification(notificationUuid, serviceUuid)
                this@EcgActivity.commandCharacteristic = commandCharacteristic
                this@EcgActivity.notificationCharacteristic = notificationCharacteristic
                mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this@EcgActivity, R.color.ble_text_blue))
                mBinding.tvConnectStatus.text = "连接成功"
            } catch (e: BleException) {
                when (e) {
                    is BleExceptionCancelTimeout -> {
                        // 提前取消超时不做处理。因为这是调用 stopAdvertising() 造成的，使用者可以直接在 stopAdvertising() 方法结束后处理 UI 的显示，不需要此回调。
                    }
                    is BleExceptionBusy -> {
                        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this@EcgActivity, R.color.ble_text_blue))
                        mBinding.tvConnectStatus.text = preState
                        Toast.makeText(this@EcgActivity, e.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        connectExecutor.disconnect()
                        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this@EcgActivity, R.color.ble_text_red))
                        mBinding.tvConnectStatus.text = e.message
                    }
                }
            }
        }
    }

    private fun disconnect() {
        try {
            connectExecutor.disconnect()
            mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_red))
            mBinding.tvConnectStatus.text = "连接断开了"
        } catch (e: BleException) {
            mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_red))
            mBinding.tvConnectStatus.text = e.message
        }
    }

}
