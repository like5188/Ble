package com.like.ble.exception

open class BleException(message: String?, val code: Int = -10000) : Exception(message)
object BleExceptionDisabled : BleException("蓝牙未打开", -10001)
object BleExceptionPermission : BleException("操作失败，缺少蓝牙权限！", -10002)
class BleExceptionDeviceDisconnected(address: String?, code: Int = -10003) : BleException("蓝牙设备未连接:$address", code)
class BleExceptionDiscoverServices(address: String?) : BleException("发现服务失败:$address", -10004)
class BleExceptionBusy(message: String? = "蓝牙繁忙，请稍后再试！") : BleException(message, -10005)
class BleExceptionTimeout(message: String?) : BleException(message, -10006)

// 提前取消超时不做处理。因为这是调用 stopScan() 或者 disconnect() 方法造成的，使用者可以直接在 stopScan() 或者 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
object BleExceptionCancelTimeout : BleException("操作被取消", -10007)

fun Exception.toBleException(): BleException {
    // 转换一下异常，方便使用者判断。
    return if (this is BleException) this else BleException(this.message)
}
