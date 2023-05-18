package com.like.ble.callback

interface OnBleEnableListener {

    /**
     * 蓝牙被打开
     */
    fun on()

    /**
     * 蓝牙被关闭
     */
    fun off()
}
