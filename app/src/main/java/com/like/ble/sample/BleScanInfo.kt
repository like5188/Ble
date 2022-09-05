package com.like.ble.sample

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import com.like.recyclerview.model.IRecyclerViewItem
import java.io.Serializable
import kotlin.math.abs
import kotlin.math.pow

class BleScanInfo(val name: String, val address: String, val rssi: ObservableInt, val scanRecord: ByteArray?) : IRecyclerViewItem,
    Serializable {
    override var variableId: Int = BR.bleScanInfo
    override var layoutId: Int = R.layout.item_ble_scan
    val distance: ObservableInt = ObservableInt(0)
    val isShowDetails: ObservableBoolean = ObservableBoolean(false)

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