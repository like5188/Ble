package com.like.ble.exception

open class BleException(message: String?, val code: Int = -1) : Exception(message)
object BleExceptionDisabled : BleException("蓝牙未打开", -2)
object BleExceptionPermission : BleException("操作失败，缺少蓝牙权限！", -3)
class BleExceptionDeviceDisconnected(address: String?) : BleException("蓝牙设备未连接:$address", -4)
class BleExceptionDiscoverServices(address: String?) : BleException("发现服务失败:$address", -5)
class BleExceptionBusy(message: String? = "蓝牙繁忙，请稍后再试！") : BleException(message, -6)
class BleExceptionTimeout(message: String?) : BleException(message, -7)

// 提前取消超时不做处理。因为这是调用 stopScan() 或者 disconnect() 方法造成的，使用者可以直接在 stopScan() 或者 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
object BleExceptionCancelTimeout : BleException("操作被取消", -8)

fun Exception.toBleException(): BleException {
    // 转换一下异常，方便使用者判断。
    return if (this is BleException) this else BleException(this.message)
}
