package com.like.ble.sample

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.like.ble.BleManager
import com.like.ble.command.*
import com.like.ble.sample.databinding.ActivityBleConnectBinding
import com.like.livedatarecyclerview.layoutmanager.WrapLinearLayoutManager

class BleConnectActivity : AppCompatActivity() {
    private val mBinding: ActivityBleConnectBinding by lazy {
        DataBindingUtil.setContentView<ActivityBleConnectBinding>(this, R.layout.activity_ble_connect)
    }
    private lateinit var mData: BleScanInfo
    private val mAdapter: BleConnectAdapter by lazy { BleConnectAdapter(this) }

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
        BleManager.sendCommand(
            ConnectCommand(
                mData.address,
                10000L,
                onResult = {
                    mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this@BleConnectActivity, R.color.ble_text_blue))
                    mBinding.tvConnectStatus.text = "连接成功"
                    if (it.isNotEmpty()) {
                        val bleGattServiceInfos = it.map { bluetoothGattService ->
                            BleConnectInfo(mData.address, bluetoothGattService)
                        }
                        mAdapter.mAdapterDataManager.addItemsToEnd(bleGattServiceInfos)
                    } else {
                        mAdapter.mAdapterDataManager.clear()
                    }
                },
                onError = {
                    mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this@BleConnectActivity, R.color.ble_text_red))
                    mBinding.tvConnectStatus.text = it.message
                    mAdapter.mAdapterDataManager.clear()
                    mBinding.etRequestMtu.setText("")
                    mBinding.etReadRemoteRssi.setText("")
                    mBinding.etRequestConnectionPriority.setText("")
                }
            ))
    }

    fun disconnect(view: View) {
        BleManager.sendCommand(DisconnectCommand(mData.address))
    }

    fun requestMtu(view: View) {
        if (mBinding.etRequestMtu.text.trim().isEmpty()) {
            shortToastBottom("请输入MTU的值")
            return
        }
        val mtu = mBinding.etRequestMtu.text.toString().trim().toInt()
        BleManager.sendCommand(RequestMtuCommand(
            mData.address,
            mtu,
            3000,
            onResult = {
                shortToastBottom("设置成功")
            },
            onError = {
                shortToastBottom(it.message)
            }
        ))
    }

    fun readRemoteRssi(view: View) {
        BleManager.sendCommand(ReadRemoteRssiCommand(
            mData.address,
            3000,
            onResult = {
                mBinding.etReadRemoteRssi.setText(it.toString())
            },
            onError = {
                shortToastBottom(it.message)
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
            BleManager.sendCommand(RequestConnectionPriorityCommand(
                mData.address,
                connectionPriority,
                onResult = {
                    shortToastBottom("设置成功")
                },
                onError = {
                    shortToastBottom(it.message)
                }
            ))
        }
    }

    override fun onDestroy() {
        BleManager.close()
        super.onDestroy()
    }

}