package com.like.ble.sample

import androidx.fragment.app.FragmentActivity
import com.like.ble.IBleManager
import com.like.ble.sample.databinding.ItemBleConnectBinding
import com.like.livedatarecyclerview.adapter.BaseAdapter
import com.like.livedatarecyclerview.model.IRecyclerViewItem
import com.like.livedatarecyclerview.viewholder.CommonViewHolder

class BleConnectAdapter(private val mActivity: FragmentActivity, private val mBleManager: IBleManager) : BaseAdapter() {

    override fun bindOtherVariable(
        holder: CommonViewHolder,
        position: Int,
        item: IRecyclerViewItem?
    ) {
        if (item !is BleConnectInfo) return
        val binding = holder.binding
        if (binding !is ItemBleConnectBinding) return
    }

}