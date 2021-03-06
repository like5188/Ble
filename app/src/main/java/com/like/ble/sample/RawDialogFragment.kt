package com.like.ble.sample

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.setPadding
import com.like.ble.sample.databinding.DialogFragmentRawBinding
import com.like.ble.utils.toHexString
import com.like.ble.utils.toHexString2

class RawDialogFragment : BaseDialogFragment<DialogFragmentRawBinding>() {

    override fun getLayoutResId(): Int {
        return R.layout.dialog_fragment_raw
    }

    override fun initView(binding: DialogFragmentRawBinding, savedInstanceState: Bundle?) {
        isCancelable = true
        resources.displayMetrics?.widthPixels?.let { screenWidth ->
            setWidth((screenWidth * 0.9).toInt())
        }
        val rawData: ByteArray = arguments?.get("data") as? ByteArray ?: return
        setData(rawData)
    }

    private fun setData(rawData: ByteArray) {
        val binding = getBinding() ?: return

        val rawDataSb = StringBuilder("0x")
        // 解析原始数据，显示详情
        val parseBleADData = parseADStructures(rawData)
        for (adStructure in parseBleADData) {
            val length = adStructure.length
            val typeHexString = adStructure.type.toHexString2()
            val dataHexString = adStructure.data.toHexString()
            rawDataSb.append(length.toHexString2()).append(typeHexString).append(dataHexString)
            addADStructureTable(length, "0x$typeHexString", "0x$dataHexString", binding.tl)
        }

        // 显示原始数据
        binding.tvRawData.text = rawDataSb.toString()
        // 复制原始数据
        binding.tvRawData.setOnClickListener {
            val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return@setOnClickListener
            val clip = ClipData.newPlainText("daqi", binding.tvRawData.text)
            clipboard.setPrimaryClip(clip)
            activity?.shortToastBottom("复制成功")
        }
    }

    /**
     * 设置广播数据单元
     */
    private fun addADStructureTable(length: Int, type: String, data: String, parent: ViewGroup) {
        activity ?: return
        //创建表格
        val tableRow: TableRow = TableRow(activity)
        //创建length视图
        val lengthView = TextView(activity)
        lengthView.layoutParams = TableRow.LayoutParams(1, ViewGroup.LayoutParams.WRAP_CONTENT)
        lengthView.setPadding(dp2px(4f).toInt())
        lengthView.text = length.toString()
        lengthView.gravity = Gravity.CENTER
        tableRow.addView(lengthView)
        //创建Type视图
        val typeView = TextView(activity)
        typeView.layoutParams = TableRow.LayoutParams(1, ViewGroup.LayoutParams.WRAP_CONTENT)
        typeView.setPadding(dp2px(4f).toInt())
        typeView.text = type
        typeView.gravity = Gravity.CENTER
        tableRow.addView(typeView)
        //创建Value视图
        val valueView = TextView(activity)
        val valueLayoutParams = TableRow.LayoutParams(1, ViewGroup.LayoutParams.WRAP_CONTENT)
        valueLayoutParams.span = 3
        valueView.layoutParams = valueLayoutParams
        valueView.setPadding(dp2px(4f).toInt())
        valueView.text = data
        valueView.gravity = Gravity.CENTER
        tableRow.addView(valueView)
        parent.addView(tableRow)
    }

    private fun dp2px(num: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, num, activity?.resources?.displayMetrics)
    }

    /**
     * 解析蓝牙广播报文，获取数据单元
     */
    private fun parseADStructures(rawData: ByteArray): List<ADStructure> {
        val aDStructures = mutableListOf<ADStructure>()
        var currentPos = 0
        while (currentPos < rawData.size) {
            val length: Int = rawData[currentPos++].toInt() and 0xFF
            if (length == 0) {
                break
            }
            // Note the length includes the length of the field type itself.
            val dataLength = length - 1
            // fieldType is unsigned int.
            val fieldType = rawData[currentPos++].toInt() and 0xFF

            val data = ByteArray(dataLength)
            System.arraycopy(rawData, currentPos, data, 0, dataLength)
            aDStructures.add(ADStructure(length, fieldType, data))
            currentPos += dataLength
        }
        return aDStructures
    }

    data class ADStructure(val length: Int, val type: Int, val data: ByteArray) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ADStructure) return false

            if (length != other.length) return false
            if (type != other.type) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = length
            result = 31 * result + type
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

}