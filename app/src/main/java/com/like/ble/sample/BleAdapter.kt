package com.like.ble.sample

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import com.like.ble.IBleManager
import com.like.ble.sample.databinding.ItemBleScanBinding
import com.like.ble.utils.deleteLast
import com.like.ble.utils.getValidString
import com.like.ble.utils.scanrecordcompat.ScanRecordBelow21
import com.like.ble.utils.toHexString
import com.like.livedatarecyclerview.adapter.BaseAdapter
import com.like.livedatarecyclerview.model.IRecyclerViewItem
import com.like.livedatarecyclerview.viewholder.CommonViewHolder

class BleAdapter(private val mActivity: Activity, private val mBleManager: IBleManager) :
    BaseAdapter() {
    private val mCommandArray =
        arrayOf(
            "读特征",
            "写特征",
            "设置MTU",
            "读RSSI",
            "RequestConnectionPriority",
            "开启notify",
            "关闭notify",
            "开启indicate",
            "关闭indicate",
            "读取通知传来的数据"
        )

    override fun bindOtherVariable(
        holder: CommonViewHolder,
        position: Int,
        item: IRecyclerViewItem?
    ) {
        if (item !is BleInfo) return
        val binding = holder.binding
        if (binding !is ItemBleScanBinding) return

        binding.tvConnect.setOnClickListener {
            val connectIntent = Intent(mActivity, ConnectActivity::class.java)
            connectIntent.putExtra("data", item)
            mActivity.startActivity(connectIntent)
        }

        binding.tvRaw.setOnClickListener {
            mActivity.shortToastBottom("原始数据")
        }

        val scanRecordCompat = ScanRecordBelow21.parseFromBytes(item.scanRecord) ?: return
        binding.tvTxPowerLevel.text = "${scanRecordCompat.txPowerLevel} dBm"

        if (scanRecordCompat.serviceUuids.isEmpty()) {
            binding.llServiceUuids.visibility = View.GONE
            binding.tvServiceUuids.text = ""
        } else {
            binding.llServiceUuids.visibility = View.VISIBLE
            val sb = StringBuilder()
            scanRecordCompat.serviceUuids.forEach {
                sb.append(it.uuid.getValidString()).append("；")
            }
            sb.deleteLast()
            binding.tvServiceUuids.text = sb.toString()
        }

        if (scanRecordCompat.manufacturerSpecificData.isEmpty()) {
            binding.llManufacturerData.visibility = View.GONE
            binding.tvManufacturerData.text = ""
        } else {
            binding.llManufacturerData.visibility = View.VISIBLE
            val sb = StringBuilder()
            scanRecordCompat.manufacturerSpecificData.forEach { key, value ->
                sb.append("id：0x${key.toHexString()}，Data：0x${value.toHexString()}").append("；")
            }
            sb.deleteLast()
            binding.tvManufacturerData.text = sb.toString()
        }

        if (scanRecordCompat.serviceData.isEmpty()) {
            binding.llServiceData.visibility = View.GONE
            binding.tvServiceData.text = ""
        } else {
            binding.llServiceData.visibility = View.VISIBLE
            val sb = StringBuilder()
            scanRecordCompat.serviceData.forEach {
                sb.append("UUID:${it.key.uuid.getValidString()}，Data：0x${it.value.toHexString()}").append("；")
            }
            sb.deleteLast()
            binding.tvServiceData.text = sb.toString()
        }
    }

}