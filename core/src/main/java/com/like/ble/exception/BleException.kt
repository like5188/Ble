package com.like.ble.exception

open class BleException(message: String?, val code: Int = -1) : Exception(message)

object BleExceptionDisabled : BleException("蓝牙未打开", -2)
object BleExceptionPermission : BleException("蓝牙权限被拒绝", -3)
class BleExceptionDeviceDisconnected(address: String?) : BleException("蓝牙设备未连接:$address", -4)
class BleExceptionBusy(message: String? = "蓝牙繁忙，请稍后再试！") : BleException(message, -5)
class BleExceptionTimeout(message: String?) : BleException(message, -6)