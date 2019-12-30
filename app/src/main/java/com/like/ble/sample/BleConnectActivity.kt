package com.like.ble.sample

import android.bluetooth.BluetoothGattService
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.like.ble.CentralManager
import com.like.ble.IBleManager
import com.like.ble.command.*
import com.like.ble.command.base.Command
import com.like.ble.sample.databinding.ActivityBleConnectBinding
import com.like.livedatarecyclerview.layoutmanager.WrapLinearLayoutManager

class BleConnectActivity : AppCompatActivity() {
    private val mBinding: ActivityBleConnectBinding by lazy {
        DataBindingUtil.setContentView<ActivityBleConnectBinding>(this, R.layout.activity_ble_connect)
    }
    private val mBleManager: IBleManager by lazy { CentralManager(this) }
    private lateinit var mData: BleScanInfo
    private val mAdapter: BleConnectAdapter by lazy { BleConnectAdapter(this, mBleManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mData = intent.getSerializableExtra("data") as? BleScanInfo ?: throw UnsupportedOperationException("data is null")
        mBinding.setVariable(BR.bleScanInfo, mData)
        mBinding.rv.layoutManager = WrapLinearLayoutManager(this)
        mBinding.rv.adapter = mAdapter
    }

    fun connect(view: View) {
        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_black_1))
        mBinding.tvConnectStatus.text = "连接中……"
        mBleManager.sendCommand(
            ConnectCommand(
                mData.address,
                10000L,
                object : Command.Callback() {
                    override fun onResult(vararg args: Any?) {
                        if (args.isNotEmpty()) {
                            val bluetoothGattServiceList = args[0] as List<BluetoothGattService>
                            mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this@BleConnectActivity, R.color.ble_text_blue))
                            mBinding.tvConnectStatus.text = "连接成功"
                            if (bluetoothGattServiceList.isNotEmpty()) {
                                val bleGattServiceInfos = bluetoothGattServiceList.map { bluetoothGattService ->
                                    BleConnectInfo(mData.address, bluetoothGattService)
                                }
                                mAdapter.mAdapterDataManager.addItemsToEnd(bleGattServiceInfos)
                            } else {
                                mAdapter.mAdapterDataManager.clear()
                            }
                        }
                    }

                    override fun onFailure(t: Throwable) {
                        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this@BleConnectActivity, R.color.ble_text_red))
                        mBinding.tvConnectStatus.text = t.message
                        mAdapter.mAdapterDataManager.clear()
                        mBinding.etRequestMtu.setText("")
                        mBinding.etReadRemoteRssi.setText("")
                        mBinding.etRequestConnectionPriority.setText("")
                    }
                })
        )
    }

    fun disconnect(view: View) {
        mBleManager.sendCommand(DisconnectCommand(mData.address))
    }

    fun requestMtu(view: View) {
        if (mBinding.etRequestMtu.text.trim().isEmpty()) {
            shortToastBottom("请输入MTU的值")
            return
        }
        val mtu = mBinding.etRequestMtu.text.toString().trim().toInt()
        mBleManager.sendCommand(RequestMtuCommand(
            mData.address,
            mtu,
            3000,
            object : Command.Callback() {
                override fun onResult(vararg args: Any?) {
                    if (args.isNotEmpty()) {
                        val mtu1 = args[0]
                        if (mtu1 is Int) {
                            shortToastBottom("设置成功")
                        }
                    }
                }

                override fun onFailure(t: Throwable) {
                    shortToastBottom(t.message)
                }
            }
        ))
    }

    fun readRemoteRssi(view: View) {
        mBleManager.sendCommand(ReadRemoteRssiCommand(
            mData.address,
            3000,
            object : Command.Callback() {
                override fun onResult(vararg args: Any?) {
                    if (args.isNotEmpty()) {
                        val rssi = args[0]
                        if (rssi is Int) {
                            mBinding.etReadRemoteRssi.setText(rssi.toString())
                        }
                    }
                }

                override fun onFailure(t: Throwable) {
                    shortToastBottom(t.message)
                }
            }
        ))
    }

    fun requestConnectionPriority(view: View) {
        if (mBinding.etRequestConnectionPriority.text.trim().isEmpty()) {
            shortToastBottom("请输入connection priority的值")
            return
        }
        val connectionPriority = mBinding.etRequestConnectionPriority.text.toString().trim().toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBleManager.sendCommand(RequestConnectionPriorityCommand(
                mData.address,
                connectionPriority,
                object : Command.Callback() {
                    override fun onResult(vararg args: Any?) {
                        if (args.isNotEmpty()) {
                            val connectionPriority1 = args[0]
                            if (connectionPriority1 is Int) {
                                shortToastBottom("设置成功")
                            }
                        }
                    }

                    override fun onFailure(t: Throwable) {
                        shortToastBottom(t.message)
                    }
                }
            ))
        }
    }

    override fun onDestroy() {
        mBleManager.close()
        super.onDestroy()
    }

}