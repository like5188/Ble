package com.like.ble.exception

open class BleException(val msg: String, val code: Int = -1) : Exception(msg)

object BleExceptionDisabled : BleException("蓝牙未打开", 0)
object BleExceptionPermission : BleException("蓝牙权限被拒绝", 1)
object BleExceptionDeviceDisconnected : BleException("蓝牙设备未连接", 2)
object BleExceptionBusy : BleException("蓝牙繁忙，请稍后再试！", 3)