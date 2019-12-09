package com.like.ble.model

import android.util.Log
import java.util.*

/**
 * 蓝牙相关的操作的返回结果。
 *
 * @param status    操作标志
 * @param data      数据
 * @param errorMsg  对失败状态的描述
 */
data class BleResult(val status: BleStatus, val data: Any? = null, val errorMsg: String = "") {
    init {
        Log.i("BleResult", this.toString())
    }

    override fun toString(): String {
        val dataString = when (data) {
            is String -> data
            is ByteArray -> Arrays.toString(data)
            else -> data.toString()
        }
        return "BleResult(status=$status, data=$dataString, errorMsg='$errorMsg')"
    }

}