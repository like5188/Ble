package com.like.ble.command

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import com.like.ble.receiver.IState

class StartAdvertisingCommand(
    val settings: AdvertiseSettings,
    val advertiseData: AdvertiseData,
    val scanResponse: AdvertiseData
) : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.startAdvertising(this)
    }

}