package com.like.ble.sample

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.like.ble.BleManager
import com.like.ble.command.InitCommand
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
    private val mBleManager: BleManager by lazy {
        BleManager(this)
    }
    private val mAdapter: BleAdapter by lazy { BleAdapter(this, mBleManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBleManager.getLiveData().observe(this, Observer {
            mBinding.tvStatus.text = it?.status?.des
        })
        mBinding.rv.layoutManager = WrapLinearLayoutManager(this)
        mBinding.rv.adapter = mAdapter
    }

    fun initBle(view: View) {
        mBleManager.sendCommand(InitCommand())
    }

    fun startScan(view: View) {
        mAdapter.mAdapterDataManager.clear()
        mBleManager.sendCommand(
            StartScanCommand(2000L, { device, rssi, scanRecord ->
                Log.d(TAG, "device=$device rssi=$rssi scanRecord=$scanRecord")
                addItem(device)
            })
        )
    }

    fun stopScan(view: View) {
        mBleManager.sendCommand(StopScanCommand())
    }

    private fun addItem(device: BluetoothDevice?) {
        val address = device?.address ?: ""
        val name = device?.name ?: "未知设备"
        if (!mAdapter.mAdapterDataManager.getAll().any { (it as BleInfo).address == address }) {
            mAdapter.mAdapterDataManager.addItemToEnd(BleInfo(name, address))
        }
    }

    override fun onDestroy() {
        mBleManager.close()
        super.onDestroy()
    }

}
