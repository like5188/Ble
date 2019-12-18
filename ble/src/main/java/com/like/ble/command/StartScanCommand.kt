package com.like.ble.command

import android.bluetooth.BluetoothDevice
import java.util.*

/**
 * 开始扫描蓝牙设备命令
 *
 * android 7.0 后不能在30秒内扫描和停止超过5次。android 蓝牙模块会打印当前应用扫描太频繁的log日志,并在android 5.0 的ScanCallback回调中触发onScanFailed(int）,返回错误码：ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,表示app无法注册，无法开始扫描）。
 *
 * @param filterDeviceName          需要过滤的设备名字，默认为空字符串，即不过滤
 * @param fuzzyMatchingDeviceName   是否模糊匹配设备名字
 * @param filterDeviceAddress       需要过滤的设备地址，默认为空字符串，即不过滤
 * @param filterServiceUuid         需要过滤的设备服务UUID，默认为null，即不过滤
 * @param timeout                   命令执行超时时间（毫秒）
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class StartScanCommand(
    val filterDeviceName: String = "",
    val fuzzyMatchingDeviceName: Boolean = true,
    val filterDeviceAddress: String = "",
    val filterServiceUuid: UUID? = null,
    val timeout: Long = 3000L,
    private val onSuccess: ((BluetoothDevice, Int, ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("开始扫描蓝牙设备命令") {

    override fun execute() {
        mReceiver?.startScan(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        if (args.size >= 3) {
            val arg0 = args[0]
            val arg1 = args[1]
            val arg2 = args[2]
            if (arg0 is BluetoothDevice && arg1 is Int && arg2 is ByteArray?) {
                onSuccess?.invoke(arg0, arg1, arg2)
            }
        }
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun getGroups(): Int = GROUP_CENTRAL or GROUP_CENTRAL_SCAN

}