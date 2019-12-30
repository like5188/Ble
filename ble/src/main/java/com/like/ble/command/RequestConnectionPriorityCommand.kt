package com.like.ble.command

import android.bluetooth.BluetoothGatt
import android.os.Build
import androidx.annotation.RequiresApi
import com.like.ble.command.base.AddressCommand

/**
 * requestConnectionPriority命令
 *
 * 快速传输大量数据时设置[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH]，完成后要设置成默认的: [android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED]
 *
 * @param connectionPriority    需要设置的priority。[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH]、[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class RequestConnectionPriorityCommand(
    address: String,
    val connectionPriority: Int,
    callback: Callback? = null
) : AddressCommand("requestConnectionPriority命令", callback = callback, address = address) {

    init {
        if (connectionPriority < BluetoothGatt.CONNECTION_PRIORITY_BALANCED ||
            connectionPriority > BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER
        ) {
            failureAndCompleteIfIncomplete("the range of connectionPriority is [0，2]")
        }
    }

    override suspend fun execute() {
        mReceiver?.requestConnectionPriority(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RequestConnectionPriorityCommand) return false
        if (!super.equals(other)) return false

        if (connectionPriority != other.connectionPriority) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + connectionPriority
        return result
    }

}