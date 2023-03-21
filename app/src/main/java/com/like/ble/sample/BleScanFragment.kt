package com.like.ble.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableInt
import androidx.lifecycle.lifecycleScope
import com.like.ble.central.scan.executor.AbstractScanExecutor
import com.like.ble.central.scan.executor.ScanExecutor
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleExceptionBusy
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
    private val scanExecutor: AbstractScanExecutor by lazy {
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
                when (it) {
                    is ScanResult.Ready -> {
                        Logger.v("ScanResult.Ready")
                        mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_blue))
                        mBinding.tvScanStatus.text = "正在开启扫描……"
                        mAdapter.submitList(null)
                    }
                    is ScanResult.Success -> {
                        Logger.d("ScanResult.Success")
                        mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_blue))
                        mBinding.tvScanStatus.text = "扫描中……"
                    }
                    is ScanResult.Completed -> {
                        Logger.i("ScanResult.Completed")
                        mBinding.tvScanStatus.text = "扫描完成"
                    }
                    is ScanResult.Error -> {
                        Logger.e("ScanResult.Error ${it.throwable}")
                        val ctx = context ?: return@collect
                        when (val e = it.throwable) {
                            is BleExceptionBusy -> {
                                Toast.makeText(ctx, e.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_red))
                                mBinding.tvScanStatus.text = it.throwable.message
                            }
                        }
                    }
                    is ScanResult.Result -> {
                        Logger.w("ScanResult.Result")
                        val name = it.device.name ?: "N/A"
                        if (name != "BLE测试设备") {// 过滤需要的外围设备
                            return@collect
                        }
                        val address = it.device.address ?: ""
                        val item: BleScanInfo? = mAdapter.currentList.firstOrNull { it?.address == address }
                        if (item == null) {// 防止重复添加
                            val newItems = mAdapter.currentList.toMutableList()
                            newItems.add(BleScanInfo(name, address, ObservableInt(it.rssi), it.scanRecord))
                            mAdapter.submitList(newItems)
                        } else {
                            item.updateRssi(it.rssi)
                        }
                    }
                }
            }
        }
        return mBinding.root
    }

    private fun startScan() {
        lifecycleScope.launch {
            scanExecutor.startScan()
        }
    }

    private fun stopScan() {
        val ctx = context ?: return
        try {
            scanExecutor.stopScan()
            mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_blue))
            mBinding.tvScanStatus.text = "扫描停止了"
        } catch (e: Exception) {
            mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_red))
            mBinding.tvScanStatus.text = e.message
        }
    }

    override fun onDestroy() {
        scanExecutor.close()
        super.onDestroy()
    }

    override fun onLazyLoadData() {
    }

}
