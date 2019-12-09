package com.like.ble.sample

import androidx.databinding.ObservableBoolean
import com.like.livedatarecyclerview.model.IItem

class BleInfo(val name: String, val address: String) : IItem {
    override var variableId: Int = BR.bleInfo
    override var layoutId: Int = R.layout.item_ble
    var isConnected = ObservableBoolean(false) // 是否连接
}