package com.like.ble.sample

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import com.like.ble.IBleManager
import com.like.ble.sample.databinding.ItemBleConnectBinding
import com.like.ble.sample.databinding.ItemBleConnectCharacteristicBinding
import com.like.ble.sample.databinding.ItemBleConnectDescriptorsBinding
import com.like.ble.utils.getPropertiesString
import com.like.ble.utils.getTypeString
import com.like.ble.utils.getValidString
import com.like.livedatarecyclerview.adapter.BaseAdapter
import com.like.livedatarecyclerview.model.IRecyclerViewItem
import com.like.livedatarecyclerview.viewholder.CommonViewHolder

class BleConnectAdapter(private val mActivity: FragmentActivity, private val mBleManager: IBleManager) : BaseAdapter() {
    private val mLayoutInflater: LayoutInflater by lazy { LayoutInflater.from(mActivity) }

    override fun bindOtherVariable(
        holder: CommonViewHolder,
        position: Int,
        item: IRecyclerViewItem?
    ) {
        if (item !is BleConnectInfo) return
        val binding = holder.binding
        if (binding !is ItemBleConnectBinding) return

        // 服务的UUID
        binding.tvServiceUuid.text = item.service.uuid.getValidString()
        // 服务的类型
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
                        addCharacteristic(it, binding.llCharacteristics)
                    }
                }
            }
        }
    }

    private fun addCharacteristic(characteristic: BluetoothGattCharacteristic, llCharacteristics: LinearLayout) {
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
                addDescriptor(it, binding.llDescriptors)
            }
        }

        if (characteristic.properties and 0x02 != 0) {
            binding.ivRead.visibility = View.VISIBLE
            binding.ivRead.setOnClickListener {
                mActivity.shortToastBottom("read characteristic")
            }
        }
        if (characteristic.properties and 0x04 != 0 || characteristic.properties and 0x08 != 0) {
            binding.ivWrite.visibility = View.VISIBLE
            binding.ivWrite.setOnClickListener {
                mActivity.shortToastBottom("write characteristic")
            }
        }
        if (characteristic.properties and 0x10 != 0) {
            binding.ivNotify.visibility = View.VISIBLE
            binding.ivNotify.setOnClickListener {
                mActivity.shortToastBottom("notify on")
            }
        }
        if (characteristic.properties and 0x20 != 0) {
            binding.ivIndicate.visibility = View.VISIBLE
            binding.ivIndicate.setOnClickListener {
                mActivity.shortToastBottom("indicate on")
            }
        }
    }

    private fun addDescriptor(descriptor: BluetoothGattDescriptor, llDescriptors: LinearLayout) {
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

        // 无法判断描述的权限，只能同时显示读和写两个操作。设置只读权限的描述，nRF也全部显示的（即显示写入和读取按钮）。
        binding.ivRead.setOnClickListener {
            mActivity.shortToastBottom("read descriptor")
        }
        binding.ivWrite.setOnClickListener {
            mActivity.shortToastBottom("write descriptor")
        }
    }

}