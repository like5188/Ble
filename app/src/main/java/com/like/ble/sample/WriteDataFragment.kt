package com.like.ble.sample

import android.os.Bundle
import com.like.ble.sample.databinding.DialogFragmentWriteDataBinding
import com.like.ble.utils.hexStringToByteArray
import com.like.ble.utils.isHexString
import java.io.Serializable

class WriteDataFragment : BaseDialogFragment<DialogFragmentWriteDataBinding>() {

    override fun getLayoutResId(): Int {
        return R.layout.dialog_fragment_write_data
    }

    override fun initView(binding: DialogFragmentWriteDataBinding, savedInstanceState: Bundle?) {
        isCancelable = true
        resources.displayMetrics?.widthPixels?.let { screenWidth ->
            setWidth((screenWidth * 0.9).toInt())
        }
        val callback: Callback? = arguments?.getSerializable("callback") as? Callback
        binding.btnConfirm.setOnClickListener {
            val data = binding.etData.text.toString().trim()
            if (data.isHexString() && data.length % 2 == 0) {
                callback?.onData(data.hexStringToByteArray())
                dismiss()
            } else {
                longToastBottom("只能输入16进制的数据")
            }
        }
    }

    interface Callback : Serializable {
        fun onData(data: ByteArray)
    }

}