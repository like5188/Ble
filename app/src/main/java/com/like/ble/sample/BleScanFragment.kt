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
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.exception.BleExceptionTimeout
import com.like.ble.sample.databinding.FragmentBleScanBinding
import com.like.common.base.BaseLazyFragment
import com.like.common.util.Logger
import com.like.recyclerview.layoutmanager.WrapLinearLayoutManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_blue))
        mBinding.tvScanStatus.text = "扫描中……"
        lifecycleScope.launch {
            scanExecutor.startScan()
                .catch {
                    Logger.e("catch scan error $it")
                    val ctx = context ?: return@catch
                    when (it) {
                        is BleExceptionCancelTimeout -> {
                            // 提前取消超时不做处理。因为这是调用 stopScan() 造成的，使用者可以直接在 stopScan() 方法结束后处理 UI 的显示，不需要此回调。
                        }
                        is BleExceptionBusy -> {
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                        is BleExceptionTimeout -> {
                            mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_blue))
                            mBinding.tvScanStatus.text = "扫描完成"
                        }
                        else -> {
                            mBinding.tvScanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_red))
                            mBinding.tvScanStatus.text = it.message
                        }
                    }
                }
                .collectLatest {
                    Logger.w("scan result ${it.device.address}")
                    val name = it.device.name ?: "N/A"
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
