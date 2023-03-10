package com.like.ble.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableInt
import com.like.ble.BleManager
import com.like.ble.central.command.StartScanCommand
import com.like.ble.central.command.StopScanCommand
import com.like.ble.central.handler.CentralCommandHandler
import com.like.ble.sample.databinding.FragmentBleScanBinding
import com.like.common.base.BaseLazyFragment
import com.like.recyclerview.layoutmanager.WrapLinearLayoutManager

/**
 * 扫描设备界面
 */
@SuppressLint("MissingPermission")
class BleScanFragment : BaseLazyFragment() {
    private lateinit var mBinding: FragmentBleScanBinding
    private val mAdapter: BleScanAdapter by lazy { BleScanAdapter(requireActivity()) }
    private val mBleManager: BleManager by lazy { BleManager(CentralCommandHandler(requireActivity())) }

    companion object {
        fun newInstance(): BleScanFragment {
            return BleScanFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_ble_scan, container, true)
        mBinding.rv.layoutManager = WrapLinearLayoutManager(requireContext())
        mBinding.rv.adapter = mAdapter
        mBinding.btnStartScan.setOnClickListener {
            startScan()
        }
        mBinding.btnStopScan.setOnClickListener {
            stopScan()
        }
        return mBinding.root
    }

    private fun startScan() {
        mBleManager.sendCommand(
            StartScanCommand(
                filterDeviceName = "BLE测试设备",// BlePeripheralActivity 中设置的外围设备蓝牙名称
                onCompleted = {
                    mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_blue))
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
                    mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
                    mBinding.tvScanStatus.text = it.message
                }
            ))
    }

    private fun stopScan() {
        mBleManager.sendCommand(StopScanCommand(
            onCompleted = {
                mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
                mBinding.tvScanStatus.text = "扫描停止了"
            },
            onError = {
                mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
                mBinding.tvScanStatus.text = it.message
            }
        ))
    }

    override fun onDestroy() {
        mBleManager.close()
        super.onDestroy()
    }

    override fun onLazyLoadData() {
    }

}
