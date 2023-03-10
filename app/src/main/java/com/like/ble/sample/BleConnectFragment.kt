package com.like.ble.sample

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.like.ble.BleManager
import com.like.ble.central.command.*
import com.like.ble.central.handler.CentralCommandHandler
import com.like.ble.sample.databinding.FragmentBleConnectBinding
import com.like.common.base.BaseLazyFragment
import com.like.recyclerview.layoutmanager.WrapLinearLayoutManager

/**
 * 连接设备界面
 */
class BleConnectFragment : BaseLazyFragment() {
    private lateinit var mBinding: FragmentBleConnectBinding
    private lateinit var mData: BleScanInfo
    private val mBleManager: BleManager by lazy { BleManager(CentralCommandHandler(requireActivity())) }
    private val mAdapter: BleConnectAdapter by lazy { BleConnectAdapter(requireActivity(), mBleManager) }

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
        mBleManager.sendCommand(
            ConnectCommand(
                mData.address,
                10000L,
                onResult = {
                    mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_blue))
                    mBinding.tvConnectStatus.text = "连接成功"
                    if (it.isNotEmpty()) {
                        val bleGattServiceInfos = it.map { bluetoothGattService ->
                            BleConnectInfo(mData.address, bluetoothGattService)
                        }
                        mAdapter.submitList(bleGattServiceInfos)
                    } else {
                        mAdapter.submitList(null)
                    }
                },
                onError = {
                    mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
                    mBinding.tvConnectStatus.text = it.message
                    mAdapter.submitList(null)
                    mBinding.etRequestMtu.setText("")
                    mBinding.etReadRemoteRssi.setText("")
                    mBinding.etRequestConnectionPriority.setText("")
                }
            ))
    }

    private fun disconnect() {
        mBleManager.sendCommand(
            DisconnectCommand(mData.address,
                onCompleted = {
                    mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
                    mBinding.tvConnectStatus.text = "连接停止了"
                },
                onError = {
                    mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_red))
                    mBinding.tvConnectStatus.text = it.message
                })
        )
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
        mBleManager.close()
        super.onDestroy()
    }

    override fun onLazyLoadData() {

    }

}