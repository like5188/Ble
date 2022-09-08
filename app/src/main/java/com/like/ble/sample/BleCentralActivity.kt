package com.like.ble.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableInt
import com.like.ble.BleManager
import com.like.ble.central.command.StartScanCommand
import com.like.ble.central.command.StopScanCommand
import com.like.ble.central.handler.CentralCommandHandler
import com.like.ble.sample.databinding.ActivityBleCentralBinding
import com.like.recyclerview.layoutmanager.WrapLinearLayoutManager

/**
 * 蓝牙测试
 */
@SuppressLint("MissingPermission")
class BleCentralActivity : AppCompatActivity() {
    companion object {
        private val TAG = BleCentralActivity::class.java.simpleName
    }

    private val mBinding: ActivityBleCentralBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_ble_central)
    }
    private val mAdapter: BleScanAdapter by lazy { BleScanAdapter(this) }
    private val mBleManager: BleManager by lazy { BleManager(CentralCommandHandler(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding.rv.layoutManager = WrapLinearLayoutManager(this)
        mBinding.rv.adapter = mAdapter
    }

    fun startScan(view: View) {
        mBleManager.sendCommand(
            StartScanCommand(
                filterDeviceName = "BLE测试设备",// BlePeripheralActivity 中设置的外围设备蓝牙名称
                onCompleted = {
                    mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_blue))
                    mBinding.tvScanStatus.text = "扫描中……"
                    mAdapter.submitList(null)
                },
                onResult = { device, rssi, scanRecord ->
                    val address = device.address ?: ""
                    val name = device.name ?: "N/A"
                    val item: BleScanInfo? = mAdapter.currentList.firstOrNull { it?.address == address }
                    if (item == null) {// 防止重复添加
                        val newItems = mAdapter.currentList.toMutableList()
                        newItems.add(BleScanInfo(name, address, ObservableInt(rssi), scanRecord))
                        mAdapter.submitList(newItems)
                    } else {
                        item.updateRssi(rssi)
                    }
                },
                onError = {
                    mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_red))
                    mBinding.tvScanStatus.text = it.message
                }
            ))
    }

    fun stopScan(view: View) {
        mBleManager.sendCommand(StopScanCommand(
            onCompleted = {
                mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_red))
                mBinding.tvScanStatus.text = "扫描停止了"
            },
            onError = {
                mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_red))
                mBinding.tvScanStatus.text = it.message
            }
        ))
    }

    override fun onDestroy() {
        mBleManager.close()
        super.onDestroy()
    }

}
