package com.like.ble.sample

import android.bluetooth.BluetoothGatt
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.like.ble.CentralManager
import com.like.ble.IBleManager
import com.like.ble.command.*
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
                {
                    runOnUiThread {
                        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_blue))
                        mBinding.tvConnectStatus.text = "连接成功"
                        if (it.isNotEmpty()) {
                            val bleGattServiceInfos = it.map { bluetoothGattService ->
                                BleConnectInfo(mData.address, bluetoothGattService)
                            }
                            mAdapter.mAdapterDataManager.addItemsToEnd(bleGattServiceInfos)
                        } else {
                            mAdapter.mAdapterDataManager.clear()
                        }
                    }
                },
                {
                    runOnUiThread {
                        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_red))
                        mBinding.tvConnectStatus.text = it.message
                        mAdapter.mAdapterDataManager.clear()
                    }
                })
        )
    }

    fun disconnect(view: View) {
        mBleManager.sendCommand(DisconnectCommand(mData.address))
    }

    fun requestMtu(view: View) {
        mBleManager.sendCommand(RequestMtuCommand(
            mData.address,
            50,
            3000,
            {
                runOnUiThread {
                    mBinding.etRequestMtu.setText(it.toString())
                }
            },
            {
                runOnUiThread {
                    mBinding.etRequestMtu.setText(it.message)
                }
            }
        ))
    }

    fun readRemoteRssi(view: View) {
        mBleManager.sendCommand(ReadRemoteRssiCommand(
            mData.address,
            3000,
            {
                runOnUiThread {
                    mBinding.etReadRemoteRssi.setText(it.toString())
                }
            },
            {
                runOnUiThread {
                    mBinding.etReadRemoteRssi.setText(it.message)
                }
            }
        ))
    }

    fun requestConnectionPriority(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBleManager.sendCommand(RequestConnectionPriorityCommand(
                mData.address,
                BluetoothGatt.CONNECTION_PRIORITY_HIGH,
                {
                    runOnUiThread {
                        mBinding.etRequestConnectionPriority.setText(it.toString())
                    }
                },
                {
                    runOnUiThread {
                        mBinding.etRequestConnectionPriority.setText(it.message)
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