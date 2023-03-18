package com.like.ble.exception

open class BleException(val msg: String?, val code: Int = -1) : Exception(msg)

object BleExceptionDisabled : BleException("蓝牙未打开", -2)
object BleExceptionPermission : BleException("蓝牙权限被拒绝", -3)
class BleExceptionDeviceDisconnected(address: String?) : BleException("蓝牙设备未连接:$address", -4)
class BleExceptionBusy(msg: String? = "蓝牙繁忙，请稍后再试！") : BleException(msg, -5)
class BleExceptionTimeout(msg: String?) : BleException(msg, -6)