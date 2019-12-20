package com.like.ble.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableInt
import com.like.ble.CentralManager
import com.like.ble.IBleManager
import com.like.ble.command.StartScanCommand
import com.like.ble.command.StopScanCommand
import com.like.ble.sample.databinding.ActivityBleCentralBinding
import com.like.livedatarecyclerview.layoutmanager.WrapLinearLayoutManager
import java.util.*

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
    private val mBleManager: IBleManager by lazy { CentralManager(this) }
    private val mAdapter: BleAdapter by lazy { BleAdapter(this, mBleManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding.rv.layoutManager = WrapLinearLayoutManager(this)
        mBinding.rv.adapter = mAdapter
    }

    fun startScan(view: View) {
        mAdapter.mAdapterDataManager.clear()
        mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_blue))
        mBinding.tvScanStatus.text = "扫描已开启"
        mBleManager.sendCommand(
            StartScanCommand(
                "",
                true,
                "",
                UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),
                { device, rssi, scanRecord ->
                    val address = device.address ?: ""
                    val name = device.name ?: "未知设备"
                    val item: BleInfo? =
                        mAdapter.mAdapterDataManager.getAll().firstOrNull { (it as? BleInfo)?.address == address } as? BleInfo
                    if (item == null) {// 防止重复添加
                        mAdapter.mAdapterDataManager.addItemToEnd(BleInfo(name, address, ObservableInt(rssi), scanRecord))
                    } else {
                        item.updateRssi(rssi)
                    }
                },
                {
                    mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_red))
                    mBinding.tvScanStatus.text = it.message ?: "扫描停止了"
                })
        )
    }

    fun stopScan(view: View) {
        mBleManager.sendCommand(StopScanCommand())
    }

    override fun onDestroy() {
        mBleManager.close()
        super.onDestroy()
    }

}
