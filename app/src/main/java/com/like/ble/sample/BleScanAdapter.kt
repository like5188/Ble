package com.like.ble.sample

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.fragment.app.FragmentActivity
import com.like.ble.IBleManager
import com.like.ble.sample.databinding.ItemBleScanBinding
import com.like.ble.utils.deleteLast
import com.like.ble.utils.getValidString
import com.like.ble.utils.scanrecordcompat.ScanRecordBelow21
import com.like.ble.utils.toHexString
import com.like.ble.utils.toHexString4
import com.like.livedatarecyclerview.adapter.BaseAdapter
import com.like.livedatarecyclerview.model.IRecyclerViewItem
import com.like.livedatarecyclerview.viewholder.CommonViewHolder

class BleScanAdapter(private val mActivity: FragmentActivity, private val mBleManager: IBleManager) : BaseAdapter() {
    private val mCommandArray = arrayOf(
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
    private val mRawDialogFragment: RawDialogFragment by lazy {
        RawDialogFragment()
    }

    override fun bindOtherVariable(
        holder: CommonViewHolder,
        position: Int,
        item: IRecyclerViewItem?
    ) {
        if (item !is BleScanInfo) return
        val binding = holder.binding
        if (binding !is ItemBleScanBinding) return

        binding.tvConnect.setOnClickListener {
            val connectIntent = Intent(mActivity, BleConnectActivity::class.java)
            connectIntent.putExtra("data", item)
            mActivity.startActivity(connectIntent)
        }

        // 单击显示原始数据
        if (item.scanRecord != null) {
            binding.tvRaw.setOnClickListener {
                mRawDialogFragment.arguments = Bundle().apply {
                    putByteArray("data", item.scanRecord)
                }
                mRawDialogFragment.show(mActivity)
            }
        }

        // 单击显示隐藏数据详情
        binding.root.setOnClickListener {
            item.isShowDetails.set(!item.isShowDetails.get())
        }

        val scanRecordCompat = ScanRecordBelow21.parseFromBytes(item.scanRecord) ?: return
        val textColor = ContextCompat.getColor(mActivity, R.color.ble_text_black_1)
        val textColorHexString = Integer.toHexString(textColor).substring(2)

        if (scanRecordCompat.txPowerLevel == Integer.MIN_VALUE) {
            binding.tvTxPowerLevel.visibility = View.GONE
            binding.tvTxPowerLevel.text = ""
        } else {
            binding.tvTxPowerLevel.visibility = View.VISIBLE
            binding.tvTxPowerLevel.text =
                Html.fromHtml(String.format("""<font color="#$textColorHexString">Tx Power Level：</font>${scanRecordCompat.txPowerLevel} dBm"""))
        }

        if (scanRecordCompat.serviceUuids.isNullOrEmpty()) {
            binding.tvServiceUuids.visibility = View.GONE
            binding.tvServiceUuids.text = ""
        } else {
            binding.tvServiceUuids.visibility = View.VISIBLE
            val sb = StringBuilder()
            scanRecordCompat.serviceUuids.forEach {
                sb.append(it.uuid.getValidString()).append("；")
            }
            sb.deleteLast()
            binding.tvServiceUuids.text =
                Html.fromHtml(String.format("""<font color="#$textColorHexString">16-bit Service UUIDs：</font>$sb"""))
        }

        if (scanRecordCompat.manufacturerSpecificData == null || scanRecordCompat.manufacturerSpecificData.isEmpty()) {
            binding.tvManufacturerData.visibility = View.GONE
            binding.tvManufacturerData.text = ""
        } else {
            binding.tvManufacturerData.visibility = View.VISIBLE
            val sb = StringBuilder()
            scanRecordCompat.manufacturerSpecificData.forEach { key, value ->
                sb.append("id:0x${key.toHexString4()}，Data:0x${value.toHexString()}").append("；")
            }
            sb.deleteLast()
            binding.tvManufacturerData.text =
                Html.fromHtml(String.format("""<font color="#$textColorHexString">Manufacturer Data：</font>$sb"""))
        }

        if (scanRecordCompat.serviceData.isNullOrEmpty()) {
            binding.tvServiceData.visibility = View.GONE
            binding.tvServiceData.text = ""
        } else {
            binding.tvServiceData.visibility = View.VISIBLE
            val sb = StringBuilder()
            scanRecordCompat.serviceData.forEach {
                sb.append("UUID:${it.key.uuid.getValidString()}，Data:0x${it.value.toHexString()}").append("；")
            }
            sb.deleteLast()
            binding.tvServiceData.text = Html.fromHtml(String.format("""<font color="#$textColorHexString">Service Data：</font>$sb"""))
        }
    }

}