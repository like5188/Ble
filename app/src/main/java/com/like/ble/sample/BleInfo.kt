package com.like.ble.sample

import androidx.databinding.ObservableBoolean
import com.like.livedatarecyclerview.model.IItem
import kotlin.math.abs
import kotlin.math.pow

class BleInfo(val name: String, val address: String, val rssi: Int, val scanRecord: ByteArray?) : IItem {
    override var variableId: Int = BR.bleInfo
    override var layoutId: Int = R.layout.item_ble
    var isConnected = ObservableBoolean(false) // 是否连接
    val distance: Int
        get() {
            return 10.0.pow((abs(rssi) - 59) / (10 * 2.0)).toInt()
        }
}