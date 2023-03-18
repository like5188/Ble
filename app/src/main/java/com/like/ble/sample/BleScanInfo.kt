package com.like.ble.sample

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import com.like.ble.util.Rssi
import com.like.ble.util.scanrecordcompat.ScanRecordBelow21
import com.like.recyclerview.model.IRecyclerViewItem
import java.io.Serializable

class BleScanInfo(val name: String, val address: String, val rssi: ObservableInt, val scanRecord: ScanRecordBelow21?) : IRecyclerViewItem,
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleScanInfo

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

}