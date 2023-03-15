package com.like.ble.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableInt
import androidx.lifecycle.lifecycleScope
import com.like.ble.central.executor.IScanExecutor
import com.like.ble.central.executor.ScanExecutor
import com.like.ble.central.result.ScanResult
import com.like.ble.result.BleResult
import com.like.ble.sample.databinding.FragmentBleScanBinding
import com.like.common.base.BaseLazyFragment
import com.like.common.util.Logger
import com.like.recyclerview.layoutmanager.WrapLinearLayoutManager
import kotlinx.coroutines.launch

/**
 * 扫描设备界面
 */
@SuppressLint("MissingPermission")
class BleScanFragment : BaseLazyFragment() {
    private lateinit var mBinding: FragmentBleScanBinding
    private val mAdapter: BleScanAdapter by lazy { BleScanAdapter(requireActivity()) }
    private val scanExecutor: IScanExecutor by lazy {
        ScanExecutor(requireActivity())
    }

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
        lifecycleScope.launch {
            scanExecutor.scanFlow.collect {
                Logger.e(it)
                when (it) {
                    is BleResult.Success<*> -> {
                        val scanResult: ScanResult = it.data as ScanResult
                        val address = scanResult.device.address ?: ""
                        val name = scanResult.device.name ?: "N/A"
                        val item: BleScanInfo? = mAdapter.currentList.firstOrNull { it?.address == address }
                        if (item == null) {// 防止重复添加
                            val newItems = mAdapter.currentList.toMutableList()
                            newItems.add(BleScanInfo(name, address, ObservableInt(scanResult.rssi), scanResult.data))
                            mAdapter.submitList(newItems)
                        } else {
                            item.updateRssi(scanResult.rssi)
                        }
                    }
                    is BleResult.Error -> {
                        mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
                        mBinding.tvScanStatus.text = it.msg
                    }
                }
            }
        }
        return mBinding.root
    }

    private fun startScan() {
        val ctx = context ?: return
        mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_blue))
        mBinding.tvScanStatus.text = "扫描中……"
        mAdapter.submitList(null)
        lifecycleScope.launch {
            scanExecutor.startScan(duration = 10000)
            if (mBinding.tvScanStatus.text != "扫描停止了") {
                mBinding.tvScanStatus.text = "扫描完成"
            }
        }
    }

    private fun stopScan() {
        try {
            scanExecutor.stopScan()
            mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_blue))
            mBinding.tvScanStatus.text = "扫描停止了"
        } catch (e: Exception) {
            mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
            mBinding.tvScanStatus.text = e.message
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch {
            scanExecutor.close()
        }
        super.onDestroy()
    }

    override fun onLazyLoadData() {
    }

}
