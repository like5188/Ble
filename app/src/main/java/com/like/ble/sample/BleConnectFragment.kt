package com.like.ble.sample

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.like.ble.central.executor.ConnectExecutor
import com.like.ble.central.executor.IConnectExecutor
import com.like.ble.sample.databinding.FragmentBleConnectBinding
import com.like.common.base.BaseLazyFragment
import com.like.recyclerview.layoutmanager.WrapLinearLayoutManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 连接设备界面
 */
class BleConnectFragment : BaseLazyFragment() {
    private lateinit var mBinding: FragmentBleConnectBinding
    private lateinit var mData: BleScanInfo
    private val connectExecutor: IConnectExecutor by lazy {
        ConnectExecutor(requireActivity())
    }
    private val mAdapter: BleConnectAdapter by lazy { BleConnectAdapter(requireActivity(), connectExecutor) }

    companion object {
        fun newInstance(bleScanInfo: BleScanInfo?): BleConnectFragment {
            return BleConnectFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("data", bleScanInfo)
                }
            }
        }
    }

    fun getBleScanInfo(): BleScanInfo? {
        return arguments?.getSerializable("data") as? BleScanInfo
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_ble_connect, container, true)
        mData = arguments?.getSerializable("data") as? BleScanInfo ?: throw UnsupportedOperationException("data is null")
        mBinding.setVariable(BR.bleScanInfo, mData)
        mBinding.rv.layoutManager = WrapLinearLayoutManager(requireContext())
        mBinding.rv.adapter = mAdapter
        mBinding.btnConnect.setOnClickListener {
            connect()
        }
        mBinding.btnDisconnect.setOnClickListener {
            disconnect()
        }
        mBinding.tvRequestMtu.setOnClickListener {
            requestMtu()
        }
        mBinding.tvReadRemoteRssi.setOnClickListener {
            readRemoteRssi()
        }
        mBinding.tvRequestConnectionPriority.setOnClickListener {
            requestConnectionPriority()
        }
        lifecycleScope.launch {
            connectExecutor.notifyFlow
                .catch {
                    longToastBottom(it.message)
                }
                .collectLatest {
                longToastBottom("读取通知传来的数据成功。数据长度：${it?.size} ${it?.contentToString()}")
            }
        }
        return mBinding.root
    }

    private fun connect() {
        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_black_1))
        mBinding.tvConnectStatus.text = "连接中……"
        mAdapter.submitList(null)
        lifecycleScope.launch {
            try {
                val services = connectExecutor.connect(mData.address, 10000L)
                mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_blue))
                mBinding.tvConnectStatus.text = "连接成功"
                if (!services.isNullOrEmpty()) {
                    val bleGattServiceInfos = services.map { bluetoothGattService ->
                        BleConnectInfo(mData.address, bluetoothGattService)
                    }
                    mAdapter.submitList(bleGattServiceInfos)
                } else {
                    mAdapter.submitList(null)
                }
            } catch (e: Exception) {
                mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
                mBinding.tvConnectStatus.text = e.message
                mAdapter.submitList(null)
                mBinding.etRequestMtu.setText("")
                mBinding.etReadRemoteRssi.setText("")
                mBinding.etRequestConnectionPriority.setText("")
            }
        }
    }

    fun disconnect() {
        try {
            connectExecutor.disconnect()
            mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
            mBinding.tvConnectStatus.text = "连接停止了"
        } catch (e: Exception) {
            mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
            mBinding.tvConnectStatus.text = e.message
        }
    }

    private fun requestMtu() {
        if (mBinding.etRequestMtu.text.trim().isEmpty()) {
            shortToastBottom("请输入MTU的值")
            return
        }
        val mtu = mBinding.etRequestMtu.text.toString().trim().toInt()
        lifecycleScope.launch {
            try {
                connectExecutor.requestMtu(mData.address, mtu, 3000)
                shortToastBottom("设置成功")
            } catch (e: Exception) {
                shortToastBottom(e.message)
            }
        }
    }

    private fun readRemoteRssi() {
        lifecycleScope.launch {
            try {
                val rssi = connectExecutor.readRemoteRssi(mData.address, 3000)
                mBinding.etReadRemoteRssi.setText(rssi.toString())
            } catch (e: Exception) {
                shortToastBottom(e.message)
            }
        }
    }

    private fun requestConnectionPriority() {
        if (mBinding.etRequestConnectionPriority.text.trim().isEmpty()) {
            shortToastBottom("请输入connection priority的值")
            return
        }
        val connectionPriority = mBinding.etRequestConnectionPriority.text.toString().trim().toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lifecycleScope.launch {
                try {
                    connectExecutor.requestConnectionPriority(mData.address, connectionPriority)
                    shortToastBottom("设置成功")
                } catch (e: Exception) {
                    shortToastBottom(e.message)
                }
            }
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch { connectExecutor.close() }
        super.onDestroy()
    }

    override fun onLazyLoadData() {

    }

}