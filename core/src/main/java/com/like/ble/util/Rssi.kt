package com.like.ble.util

import kotlin.math.abs
import kotlin.math.pow

class Rssi {
    companion object {
        /**
         * 把 rssi 值转换成距离
         */
        fun toDistance(rssi: Int): Double {
            return 10.0.pow((abs(rssi) - 59) / (10 * 2.0))
        }
    }
}