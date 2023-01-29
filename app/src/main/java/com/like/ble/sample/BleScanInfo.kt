package com.like.ble.sample

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import com.like.ble.util.Rssi
import com.like.recyclerview.model.IRecyclerViewItem
import java.io.Serializable

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
        distance.set(Rssi.toDistance(rssi).toInt())
    }
}