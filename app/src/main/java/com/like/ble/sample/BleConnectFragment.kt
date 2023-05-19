package com.like.ble.sample

import android.bluetooth.BluetoothGattService
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.like.ble.callback.OnBleEnableListener
import com.like.ble.central.connect.executor.AbstractConnectExecutor
import com.like.ble.central.connect.executor.ConnectExecutorFactory
import com.like.ble.central.scan.executor.ScanExecutorFactory
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.sample.databinding.FragmentBleConnectBinding
import com.like.common.util.Logger
import com.like.recyclerview.layoutmanager.WrapLinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * 连接设备界面
 */
class BleConnectFragment : Fragment() {
    private lateinit var mBinding: FragmentBleConnectBinding
    private lateinit var mData: BleScanInfo
    private val connectExecutor: AbstractConnectExecutor by lazy {
        ConnectExecutorFactory.get(requireContext(), mData.address).apply {
            setOnBleEnableListener(object : OnBleEnableListener {
                override fun on() {
                    if (mBinding.tvConnectStatus.text == "未连接") {
                        return
                    }
                    reConnect()
                }

                override fun off() {
                    needScan = true
                    connectExecutor.disconnect()
                    val ctx = context ?: return
                    val redColor = ContextCompat.getColor(ctx, R.color.ble_text_red)
                    mBinding.tvConnectStatus.setTextColor(redColor)
                    mBinding.tvConnectStatus.text = "蓝牙未打开"
                }
            })
        }
    }
    private val mAdapter: BleConnectAdapter by lazy { BleConnectAdapter(requireActivity(), connectExecutor) }
    private var needScan: Boolean = false

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
        connectExecutor.requestEnvironment(activity)
        return mBinding.root
    }

    private fun connect() {
        val preState = mBinding.tvConnectStatus.text
        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ble_text_black_1))
        mBinding.tvConnectStatus.text = "连接中……"

        fun onConnectSuccess(services: List<BluetoothGattService>) {
            val ctx = context ?: return
            lifecycleScope.launch(Dispatchers.Main) {
                mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_blue))
                mBinding.tvConnectStatus.text = "连接成功"
                if (services.isNotEmpty()) {
                    val bleGattServiceInfos = services.map { bluetoothGattService ->
                        BleConnectInfo(mData.address, bluetoothGattService)
                    }
                    mAdapter.submitList(bleGattServiceInfos)
                } else {
                    mAdapter.submitList(null)
                }
            }
        }

        fun onDisconnected(throwable: Throwable) {
            val ctx = context ?: return
            lifecycleScope.launch(Dispatchers.Main) {
                when (throwable) {
                    is BleExceptionCancelTimeout -> {
                        // 提前取消超时(BleExceptionCancelTimeout)不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                    }
                    is BleExceptionBusy -> {
                        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_blue))
                        mBinding.tvConnectStatus.text = preState
                        Toast.makeText(ctx, throwable.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_red))
                        mBinding.tvConnectStatus.text = throwable.message
                        mAdapter.submitList(null)
                        mBinding.etRequestMtu.setText("")
                        mBinding.etReadRemoteRssi.setText("")
                        mBinding.etRequestConnectionPriority.setText("")
                        reConnect()
                    }
                }
            }
        }

        lifecycleScope.launch {
            try {
                var scanResult: ScanResult? = null
                if (needScan) {
                    scanResult = ScanExecutorFactory.get(requireContext()).startScan()
                        .catch {
                            when (it) {
                                is BleExceptionCancelTimeout -> {
                                }
                                else -> {
                                    throw BleException("连接蓝牙失败，未找到蓝牙设备：${mData.address}")
                                }
                            }
                        }
                        .firstOrNull {
                            Logger.v(it)
                            it.device.address == mData.address
                        }
                    if (scanResult != null) {
                        needScan = false
                        ScanExecutorFactory.get(requireContext()).stopScan()
                    }
                }
                val services = connectExecutor.connect(scanResult?.device) {
                    // 连接成功后再断开
                    onDisconnected(it)
                }
                onConnectSuccess(services)
            } catch (e: BleException) {
                onDisconnected(e)
            }
        }
    }

    private fun reConnect() {
        mBinding.tvConnectStatus.postDelayed({
            this@BleConnectFragment.connect()
        }, 3000)
    }

    fun disconnect() {
        val ctx = context ?: return
        try {
            connectExecutor.disconnect()
            mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_red))
            mBinding.tvConnectStatus.text = "连接断开了"
        } catch (e: BleException) {
            mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(ctx, R.color.ble_text_red))
            mBinding.tvConnectStatus.text = e.message
        }
    }

    private fun requestMtu() {
        if (mBinding.etRequestMtu.text.trim().isEmpty()) {
            Toast.makeText(context, "请输入MTU的值", Toast.LENGTH_SHORT).show()
            return
        }
        val mtu = mBinding.etRequestMtu.text.toString().trim().toInt()
        lifecycleScope.launch {
            try {
                connectExecutor.requestMtu(mtu)
                Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show()
            } catch (e: BleException) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readRemoteRssi() {
        lifecycleScope.launch {
            try {
                val rssi = connectExecutor.readRemoteRssi(3000)
                mBinding.etReadRemoteRssi.setText(rssi.toString())
            } catch (e: BleException) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestConnectionPriority() {
        if (mBinding.etRequestConnectionPriority.text.trim().isEmpty()) {
            Toast.makeText(context, "请输入connection priority的值", Toast.LENGTH_SHORT).show()
            return
        }
        val connectionPriority = mBinding.etRequestConnectionPriority.text.toString().trim().toInt()
        lifecycleScope.launch {
            try {
                connectExecutor.requestConnectionPriority(connectionPriority)
                Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show()
            } catch (e: BleException) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        connectExecutor.close()
        super.onDestroy()
    }

}