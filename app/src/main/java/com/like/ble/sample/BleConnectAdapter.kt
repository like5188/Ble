package com.like.ble.sample

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import com.like.ble.central.executor.IConnectExecutor
import com.like.ble.sample.databinding.ItemBleConnectBinding
import com.like.ble.sample.databinding.ItemBleConnectCharacteristicBinding
import com.like.ble.sample.databinding.ItemBleConnectDescriptorsBinding
import com.like.ble.util.batch
import com.like.ble.util.getPropertiesString
import com.like.ble.util.getTypeString
import com.like.ble.util.getValidString
import com.like.recyclerview.adapter.BaseListAdapter
import com.like.recyclerview.viewholder.BindingViewHolder
import kotlinx.coroutines.launch
import java.util.*

class BleConnectAdapter(private val mActivity: FragmentActivity, private val connectExecutor: IConnectExecutor) :
    BaseListAdapter<ItemBleConnectBinding, BleConnectInfo>(DIFF) {
    private val mLayoutInflater: LayoutInflater by lazy { LayoutInflater.from(mActivity) }
    private val mWriteDataFragment: WriteDataFragment by lazy { WriteDataFragment() }

    override fun onBindViewHolder(holder: BindingViewHolder<ItemBleConnectBinding>, item: BleConnectInfo?) {
        super.onBindViewHolder(holder, item)
        item ?: return
        val binding = holder.binding

        // 服务UUID
        binding.tvServiceUuid.text = item.service.uuid.getValidString()
        // 服务类型
        binding.tvServiceType.text = item.service.getTypeString()
        // 服务名称
        when (item.service.uuid.toString()) {
            "00001801-0000-1000-8000-00805f9b34fb" -> {
                binding.tvServiceName.text = "Generic Attribute"
            }
            "00001800-0000-1000-8000-00805f9b34fb" -> {
                binding.tvServiceName.text = "Generic Access"
            }
            else -> {
                binding.tvServiceName.text = "Unknown Service"
                // 添加BluetoothGattCharacteristic
                binding.llCharacteristics.removeAllViews()
                val characteristics = item.service.characteristics ?: return
                if (characteristics.isNotEmpty()) {
                    binding.llService.setOnClickListener {
                        if (binding.llCharacteristics.visibility == View.GONE) {
                            binding.llCharacteristics.visibility = View.VISIBLE
                        } else {
                            binding.llCharacteristics.visibility = View.GONE
                        }
                    }
                    characteristics.forEach {
                        addCharacteristic(item.address, item.service.uuid, it, binding.llCharacteristics)
                    }
                }
            }
        }
    }

    private fun addCharacteristic(
        address: String,
        serviceUuid: UUID,
        characteristic: BluetoothGattCharacteristic,
        llCharacteristics: LinearLayout
    ) {
        val binding = DataBindingUtil.inflate<ItemBleConnectCharacteristicBinding>(
            mLayoutInflater,
            R.layout.item_ble_connect_characteristic,
            llCharacteristics,
            false
        )

        // 特征名称
        binding.tvCharacteristicName.text = "Unknown Characteristic"
        // 特征uuid
        binding.tvCharacteristicUuid.text = characteristic.uuid.getValidString()
        // 特征属性
        binding.tvCharacteristicProperties.text = characteristic.getPropertiesString()
        //将当前特征具体的布局添加到特征容器布局中
        llCharacteristics.addView(binding.root)

        // 添加BluetoothGattDescriptor
        binding.llDescriptors.removeAllViews()
        val descriptors = characteristic.descriptors ?: return
        if (descriptors.isEmpty()) {
            binding.tvDescriptorsTag.visibility = View.GONE
        } else {
            binding.tvDescriptorsTag.visibility = View.VISIBLE
            descriptors.forEach {
                addDescriptor(address, serviceUuid, characteristic, it, binding.llDescriptors)
            }
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
            binding.ivRead.visibility = View.VISIBLE
            binding.ivRead.setOnClickListener {
                mActivity.lifecycleScope.launch {
                    try {
                        val data = connectExecutor.readCharacteristic(address, characteristic.uuid, serviceUuid, 10000)
                        mActivity.longToastBottom("读特征成功。数据长度：${data?.size} ${data?.contentToString()}")
                    } catch (e: Exception) {
                        mActivity.longToastBottom(e.message)
                    }
                }
            }
        }
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0 ||
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
        ) {
            binding.ivWrite.visibility = View.VISIBLE
            binding.ivWrite.setOnClickListener {
                mWriteDataFragment.arguments = Bundle().apply {
                    putSerializable("callback", object : WriteDataFragment.Callback {
                        override fun onData(data: ByteArray) {
                            mActivity.lifecycleScope.launch {
                                try {
                                    connectExecutor.writeCharacteristic(
                                        address,
                                        data.batch(20),
                                        characteristic.uuid,
                                        serviceUuid,
                                        5000,
                                    )
                                    mActivity.longToastBottom("写特征成功")
                                } catch (e: Exception) {
                                    mActivity.longToastBottom(e.message)
                                }
                            }
                        }
                    })
                }
                mWriteDataFragment.show(mActivity)
            }
        }
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            binding.ivNotify.visibility = View.VISIBLE
            var isOn = false
            binding.ivNotify.setOnClickListener {
                mActivity.lifecycleScope.launch {
                    try {
                        val data = connectExecutor.setCharacteristicNotification(
                            address,
                            characteristic.uuid,
                            serviceUuid,
                            0,
                            !isOn,
                        )
                        if (data) {
                            isOn = !isOn
                            if (isOn) {
                                binding.ivNotify.setImageResource(R.drawable.notify)
                            } else {
                                binding.ivNotify.setImageResource(R.drawable.notify_close)
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        }
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            binding.ivIndicate.visibility = View.VISIBLE
            var isOn = false
            binding.ivIndicate.setOnClickListener {
                mActivity.lifecycleScope.launch {
                    try {
                        val data = connectExecutor.setCharacteristicNotification(
                            address,
                            characteristic.uuid,
                            serviceUuid,
                            1,
                            !isOn,
                        )
                        if (data) {
                            isOn = !isOn
                            if (isOn) {
                                binding.ivIndicate.setImageResource(R.drawable.indicate)
                            } else {
                                binding.ivIndicate.setImageResource(R.drawable.indicate_close)
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    private fun addDescriptor(
        address: String,
        serviceUuid: UUID,
        characteristic: BluetoothGattCharacteristic,
        descriptor: BluetoothGattDescriptor,
        llDescriptors: LinearLayout
    ) {
        val binding = DataBindingUtil.inflate<ItemBleConnectDescriptorsBinding>(
            mLayoutInflater,
            R.layout.item_ble_connect_descriptors,
            llDescriptors,
            false
        )
        // 描述名称
        binding.tvDescriptorName.text = "Unknown Descriptor"
        // 描述uuid
        binding.tvDescriptorUuid.text = descriptor.uuid.getValidString()
        llDescriptors.addView(binding.root)

        if ("00002902-0000-1000-8000-00805f9b34fb" == descriptor.uuid.toString()) {// 设置通知的描述，只读
            binding.ivWrite.visibility = View.GONE
        }

        // 无法判断描述的权限，只能同时显示读和写两个操作。设置只读权限的描述，nRF也全部显示的（即显示写入和读取按钮）。
        binding.ivRead.setOnClickListener {
            mActivity.lifecycleScope.launch {
                try {
                    val data = connectExecutor.readDescriptor(
                        address,
                        descriptor.uuid,
                        characteristic.uuid,
                        serviceUuid,
                        10000,
                    )
                    mActivity.longToastBottom("读描述成功。数据长度：${data?.size} ${data?.contentToString()}")
                } catch (e: Exception) {
                    mActivity.longToastBottom(e.message)
                }
            }
        }
        binding.ivWrite.setOnClickListener {
            mWriteDataFragment.arguments = Bundle().apply {
                putSerializable("callback", object : WriteDataFragment.Callback {
                    override fun onData(data: ByteArray) {
                        mActivity.lifecycleScope.launch {
                            try {
                                connectExecutor.writeDescriptor(
                                    address,
                                    data.batch(20),
                                    descriptor.uuid,
                                    characteristic.uuid,
                                    serviceUuid,
                                    5000,
                                )
                                mActivity.longToastBottom("写描述成功")
                            } catch (e: Exception) {
                                mActivity.longToastBottom(e.message)
                            }
                        }
                    }
                })
            }
            mWriteDataFragment.show(mActivity)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BleConnectInfo>() {
            override fun areItemsTheSame(oldItem: BleConnectInfo, newItem: BleConnectInfo): Boolean {
                return oldItem.address == newItem.address
            }

            override fun areContentsTheSame(oldItem: BleConnectInfo, newItem: BleConnectInfo): Boolean {
                return oldItem.address == newItem.address
            }
        }
    }

}