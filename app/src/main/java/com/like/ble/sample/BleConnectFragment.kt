package com.like.ble.sample

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.like.ble.BleManager
import com.like.ble.central.command.ReadRemoteRssiCommand
import com.like.ble.central.command.RequestConnectionPriorityCommand
import com.like.ble.central.command.RequestMtuCommand
import com.like.ble.central.executor.ConnectExecutor
import com.like.ble.central.executor.IConnectExecutor
import com.like.ble.sample.databinding.FragmentBleConnectBinding
import com.like.common.base.BaseLazyFragment
import com.like.recyclerview.layoutmanager.WrapLinearLayoutManager
import kotlinx.coroutines.launch

/**
 * 连接设备界面
 */
class BleConnectFragment : BaseLazyFragment() {
    private lateinit var mBinding: FragmentBleConnectBinding
    private lateinit var mData: BleScanInfo
    private val mBleManager: BleManager by lazy { (requireActivity() as BleCentralActivity).mBleManager }
    private val mAdapter: BleConnectAdapter by lazy { BleConnectAdapter(requireActivity(), mBleManager) }
    private val connectExecutor: IConnectExecutor by lazy {
        ConnectExecutor(requireActivity())
    }

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
        lifecycleScope.launch {
            try {
                connectExecutor.disconnect()
                mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
                mBinding.tvConnectStatus.text = "连接停止了"
            } catch (e: Exception) {
                mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
                mBinding.tvConnectStatus.text = e.message
            }
        }
    }

    private fun requestMtu() {
        if (mBinding.etRequestMtu.text.trim().isEmpty()) {
            shortToastBottom("请输入MTU的值")
            return
        }
        val mtu = mBinding.etRequestMtu.text.toString().trim().toInt()
        mBleManager.sendCommand(RequestMtuCommand(
            mData.address,
            mtu,
            3000,
            onResult = {
                shortToastBottom("设置成功")
            },
            onError = {
                shortToastBottom(it.message)
            }
        ))
    }

    private fun readRemoteRssi() {
        mBleManager.sendCommand(ReadRemoteRssiCommand(
            mData.address,
            3000,
            onResult = {
                mBinding.etReadRemoteRssi.setText(it.toString())
            },
            onError = {
                shortToastBottom(it.message)
            }
        ))
    }

    private fun requestConnectionPriority() {
        if (mBinding.etRequestConnectionPriority.text.trim().isEmpty()) {
            shortToastBottom("请输入connection priority的值")
            return
        }
        val connectionPriority = mBinding.etRequestConnectionPriority.text.toString().trim().toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBleManager.sendCommand(RequestConnectionPriorityCommand(
                mData.address,
                connectionPriority,
                onResult = {
                    shortToastBottom("设置成功")
                },
                onError = {
                    shortToastBottom(it.message)
                }
            ))
        }
    }

    override fun onDestroy() {
        mBleManager.closeConnect(mData.address)
        super.onDestroy()
    }

    override fun onLazyLoadData() {

    }

}