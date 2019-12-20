package com.like.ble.sample

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import com.like.livedatarecyclerview.model.IItem
import kotlin.math.abs
import kotlin.math.pow

class BleInfo(val name: String, val address: String, var rssi: ObservableInt, val scanRecord: ByteArray?) : IItem {
    override var variableId: Int = BR.bleInfo
    override var layoutId: Int = R.layout.item_ble
    var isConnected = ObservableBoolean(false) // 是否连接
    val distance: ObservableInt = ObservableInt(0)

    init {
        updateDistance(rssi.get())
    }

    fun updateRssi(rssi: Int) {
        this.rssi.set(rssi)
        updateDistance(rssi)
    }

    private fun updateDistance(rssi: Int) {
        distance.set(10.0.pow((abs(rssi) - 59) / (10 * 2.0)).toInt())
    }
}