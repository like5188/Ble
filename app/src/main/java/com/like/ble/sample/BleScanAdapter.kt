package com.like.ble.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import com.like.ble.central.util.scanrecordcompat.ScanRecordBelow21
import com.like.ble.sample.databinding.ItemBleScanBinding
import com.like.ble.util.deleteLast
import com.like.ble.util.getValidString
import com.like.ble.util.toHexString
import com.like.ble.util.toHexString4
import com.like.recyclerview.adapter.BaseListAdapter
import com.like.recyclerview.viewholder.BindingViewHolder

class BleScanAdapter(private val mActivity: FragmentActivity) : BaseListAdapter<ItemBleScanBinding, BleScanInfo>(DIFF) {
    private val mRawDialogFragment: RawDialogFragment by lazy {
        RawDialogFragment()
    }

    override fun onBindViewHolder(holder: BindingViewHolder<ItemBleScanBinding>, item: BleScanInfo?) {
        super.onBindViewHolder(holder, item)
        item ?: return
        val binding = holder.binding

        binding.tvConnect.setOnClickListener {
            val bleCentralActivity = mActivity as BleCentralActivity
            bleCentralActivity.addBleConnectFragment(item)
        }

        // 单击显示隐藏数据详情
        binding.root.setOnClickListener {
            item.isShowDetails.set(!item.isShowDetails.get())
        }

        // 单击显示原始数据
        val scanRecordBelow21 = ScanRecordBelow21.parseFromBytes(item.scanRecord) ?: return
        binding.tvRaw.setOnClickListener {
            mRawDialogFragment.arguments = Bundle().apply {
                putByteArray("data", item.scanRecord)
            }
            mRawDialogFragment.show(mActivity)
        }

        val textColor = ContextCompat.getColor(mActivity, R.color.ble_text_black_1)
        val textColorHexString = Integer.toHexString(textColor).substring(2)

        // 判断是厂商类型
        when {
            scanRecordBelow21.getManufacturerSpecificData(0x4C) != null -> binding.ivManufacturerType.setImageResource(R.drawable.apple)
            scanRecordBelow21.getManufacturerSpecificData(0x06) != null -> binding.ivManufacturerType.setImageResource(R.drawable.windows)
            else -> binding.ivManufacturerType.setImageResource(R.drawable.bluetooth)
        }

        if (scanRecordBelow21.txPowerLevel == Integer.MIN_VALUE) {
            binding.tvTxPowerLevel.visibility = View.GONE
            binding.tvTxPowerLevel.text = ""
        } else {
            binding.tvTxPowerLevel.visibility = View.VISIBLE
            binding.tvTxPowerLevel.text =
                Html.fromHtml(String.format("""<font color="#$textColorHexString">Tx Power Level：</font>${scanRecordBelow21.txPowerLevel} dBm"""))
        }

        if (scanRecordBelow21.serviceUuids.isNullOrEmpty()) {
            binding.tvServiceUuids.visibility = View.GONE
            binding.tvServiceUuids.text = ""
        } else {
            binding.tvServiceUuids.visibility = View.VISIBLE
            val sb = StringBuilder()
            scanRecordBelow21.serviceUuids.forEach {
                sb.append(it.uuid.getValidString()).append("；")
            }
            sb.deleteLast()
            binding.tvServiceUuids.text =
                Html.fromHtml(String.format("""<font color="#$textColorHexString">16-bit Service UUIDs：</font>$sb"""))
        }

        if (scanRecordBelow21.manufacturerSpecificData == null || scanRecordBelow21.manufacturerSpecificData.isEmpty()) {
            binding.tvManufacturerData.visibility = View.GONE
            binding.tvManufacturerData.text = ""
        } else {
            binding.tvManufacturerData.visibility = View.VISIBLE
            val sb = StringBuilder()
            scanRecordBelow21.manufacturerSpecificData.forEach { key, value ->
                sb.append("id:0x${key.toHexString4()}，Data:0x${value.toHexString()}").append("；")
            }
            sb.deleteLast()
            binding.tvManufacturerData.text =
                Html.fromHtml(String.format("""<font color="#$textColorHexString">Manufacturer Data：</font>$sb"""))
        }

        if (scanRecordBelow21.serviceData.isNullOrEmpty()) {
            binding.tvServiceData.visibility = View.GONE
            binding.tvServiceData.text = ""
        } else {
            binding.tvServiceData.visibility = View.VISIBLE
            val sb = StringBuilder()
            scanRecordBelow21.serviceData.forEach {
                sb.append("UUID:${it.key.uuid.getValidString()}，Data:0x${it.value.toHexString()}").append("；")
            }
            sb.deleteLast()
            binding.tvServiceData.text = Html.fromHtml(String.format("""<font color="#$textColorHexString">Service Data：</font>$sb"""))
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BleScanInfo>() {
            override fun areItemsTheSame(oldItem: BleScanInfo, newItem: BleScanInfo): Boolean {
                return oldItem.address == newItem.address
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: BleScanInfo, newItem: BleScanInfo): Boolean {
                return oldItem.name == newItem.name &&
                        oldItem.rssi == newItem.rssi &&
                        oldItem.scanRecord.contentEquals(newItem.scanRecord) &&
                        oldItem.distance == newItem.distance &&
                        oldItem.isShowDetails == newItem.isShowDetails
            }
        }
    }
}