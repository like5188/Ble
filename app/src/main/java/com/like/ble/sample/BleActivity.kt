package com.like.ble.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.like.ble.BleManager
import com.like.ble.command.concrete.InitCommand
import com.like.ble.command.concrete.StartScanCommand
import com.like.ble.command.concrete.StopScanCommand
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
    private val mBleManager: BleManager by lazy {
        BleManager(this)
    }
    private val mAdapter: BleAdapter by lazy { BleAdapter(this, mBleManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding.rv.layoutManager = WrapLinearLayoutManager(this)
        mBinding.rv.adapter = mAdapter
    }

    fun initBle(view: View) {
        mAdapter.mAdapterDataManager.clear()
        mBleManager.sendCommand(
            InitCommand({
                mBinding.tvStatus.text = "初始化成功"
            }, {
                mBinding.tvStatus.text = "初始化失败"
            })
        )
    }

    fun startScan(view: View) {
        mAdapter.mAdapterDataManager.clear()
        mBleManager.sendCommand(
            StartScanCommand(2000L) { device, rssi, scanRecord ->
                Log.d(TAG, "scanRecord=$scanRecord")
                val address = device.address ?: ""
                val name = device.name ?: "未知设备"
                if (!mAdapter.mAdapterDataManager.getAll().any { (it as BleInfo).address == address }) {// 防止重复添加
                    mAdapter.mAdapterDataManager.addItemToEnd(BleInfo(name, address, rssi, scanRecord))
                }
            }
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
