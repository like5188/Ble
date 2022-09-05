package com.like.ble.sample

import android.bluetooth.BluetoothGattService
import com.like.recyclerview.model.IRecyclerViewItem

class BleConnectInfo(val address: String, val service: BluetoothGattService) : IRecyclerViewItem {
    override var variableId: Int = BR.bleConnectInfo
    override var layoutId: Int = R.layout.item_ble_connect
}