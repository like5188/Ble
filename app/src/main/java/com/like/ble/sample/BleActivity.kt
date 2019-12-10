package com.like.ble.sample

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.like.ble.BleManager
import com.like.ble.model.BleStartScanCommand
import com.like.ble.model.BleStopScanCommand
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
        mBleManager.initBle()
    }

    fun startScan(view: View) {
        mAdapter.mAdapterDataManager.clear()
        mBleManager.sendCommand(
            BleStartScanCommand(2000L, {
                Log.d(TAG, "device=${it?.device} rssi=${it?.rssi} scanRecord=${it?.scanRecord}")
                addItem(it?.device)
            })
        )
    }

    fun stopScan(view: View) {
        mBleManager.sendCommand(BleStopScanCommand())
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
