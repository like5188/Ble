package com.like.ble.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableInt
import com.like.ble.BleManager
import com.like.ble.command.StartScanCommand
import com.like.ble.command.StopScanCommand
import com.like.ble.executor.CentralExecutor
import com.like.ble.sample.databinding.ActivityBleCentralBinding
import com.like.livedatarecyclerview.layoutmanager.WrapLinearLayoutManager

/**
 * 蓝牙测试
 */
class BleCentralActivity : AppCompatActivity() {
    companion object {
        private val TAG = BleCentralActivity::class.java.simpleName
    }

    private val mBinding: ActivityBleCentralBinding by lazy {
        DataBindingUtil.setContentView<ActivityBleCentralBinding>(this, R.layout.activity_ble_central)
    }
    private val mAdapter: BleScanAdapter by lazy { BleScanAdapter(this) }
    private val mBleManager: BleManager by lazy { BleManager(CentralExecutor(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding.rv.layoutManager = WrapLinearLayoutManager(this)
        mBinding.rv.adapter = mAdapter
    }

    fun startScan(view: View) {
        mBleManager.sendCommand(
            StartScanCommand(
                onCompleted = {
                    mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_blue))
                    mBinding.tvScanStatus.text = "扫描已开启"
                    mAdapter.mAdapterDataManager.clear()
                },
                onResult = { device, rssi, scanRecord ->
                    val address = device.address ?: ""
                    val name = device.name ?: "N/A"
                    val item: BleScanInfo? =
                        mAdapter.mAdapterDataManager.getAll().firstOrNull { (it as? BleScanInfo)?.address == address } as? BleScanInfo
                    if (item == null) {// 防止重复添加
                        mAdapter.mAdapterDataManager.addItemToEnd(BleScanInfo(name, address, ObservableInt(rssi), scanRecord))
                    } else {
                        item.updateRssi(rssi)
                    }
                },
                onError = {
                    mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(this@BleCentralActivity, R.color.ble_text_red))
                    mBinding.tvScanStatus.text = it.message ?: "扫描停止了"
                }
            ))
    }

    fun stopScan(view: View) {
        mBleManager.sendCommand(StopScanCommand())
    }

    override fun onDestroy() {
        mBleManager.close()
        super.onDestroy()
    }

}
