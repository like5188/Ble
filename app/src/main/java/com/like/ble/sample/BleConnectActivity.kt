package com.like.ble.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.like.ble.CentralManager
import com.like.ble.IBleManager
import com.like.ble.command.ConnectCommand
import com.like.ble.command.DisconnectCommand
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
        mBinding.rv.layoutManager = WrapLinearLayoutManager(this)
        mBinding.rv.adapter = mAdapter

        mBinding.subtitleCollapsingToolbarLayout.title = mData.name
        mBinding.subtitleCollapsingToolbarLayout.subtitle = mData.address

        // 填充menu
        mBinding.toolbar.inflateMenu(R.menu.connect_menu)
        // 设置点击事件
        mBinding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.connect -> {
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
                R.id.disconnect -> {
                    mBleManager.sendCommand(DisconnectCommand(mData.address))
                }
            }
            true
        }
    }

    override fun onDestroy() {
        mBleManager.close()
        super.onDestroy()
    }

}