package com.like.ble.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.like.ble.sample.databinding.DialogFragmentWriteDataBinding
import com.like.ble.util.hexStringToByteArray
import com.like.ble.util.isHexString
import com.like.common.base.BaseDialogFragment
import java.io.Serializable

class WriteDataFragment : BaseDialogFragment() {
    private lateinit var mBinding: DialogFragmentWriteDataBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_fragment_write_data, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callback: Callback? = arguments?.getSerializable("callback") as? Callback
        mBinding.btnConfirm.setOnClickListener {
            val data = mBinding.etData.text.toString().trim()
            if (data.isHexString() && data.length % 2 == 0) {
                callback?.onData(data.hexStringToByteArray())
                dismiss()
            } else {
                longToastBottom("只能输入16进制的数据")
            }
        }
    }

    override fun initLayoutParams(layoutParams: WindowManager.LayoutParams) {
        super.initLayoutParams(layoutParams)
        resources.displayMetrics?.widthPixels?.let { screenWidth ->
            // 宽高
            layoutParams.width = (screenWidth * 0.9).toInt()
        }
    }

    interface Callback : Serializable {
        fun onData(data: ByteArray)
    }

}