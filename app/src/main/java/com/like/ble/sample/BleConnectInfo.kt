package com.like.ble.sample

import android.bluetooth.BluetoothGattService
import com.like.livedatarecyclerview.model.IItem

class BleConnectInfo(val address: String, val service: BluetoothGattService) : IItem {
    override var variableId: Int = BR.bleConnectInfo
    override var layoutId: Int = R.layout.item_ble_connect
}