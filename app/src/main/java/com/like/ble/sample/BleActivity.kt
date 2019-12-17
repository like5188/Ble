package com.like.ble.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.like.ble.CentralManager
import com.like.ble.IBleManager
import com.like.ble.command.StartScanCommand
import com.like.ble.command.StopScanCommand
import com.like.ble.sample.databinding.ActivityBleBinding
import com.like.livedatarecyclerview.layoutmanager.WrapLinearLayoutManager


/**
 * 蓝牙测试
 */
class BleActivity : AppCompatActivity() {
    companion object {
        private val TAG = BleActivity::class.java.simpleName
    }

    private val mBinding: ActivityBleBinding by lazy {
        DataBindingUtil.setContentView<ActivityBleBinding>(this, R.layout.activity_ble)
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
        mBleManager.sendCommand(
            StartScanCommand(
                "MEIZU",
                true,
                "",
                2000L,
                { device, rssi, scanRecord ->
                    val address = device.address ?: ""
                    val name = device.name ?: "未知设备"
                    if (!mAdapter.mAdapterDataManager.getAll().any { (it as BleInfo).address == address }) {// 防止重复添加
                        mAdapter.mAdapterDataManager.addItemToEnd(BleInfo(name, address, rssi, scanRecord))
                    }
                },
                {
                    shortToastCenter(it.message)
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
