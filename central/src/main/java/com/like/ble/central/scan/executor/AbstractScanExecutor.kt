package com.like.ble.central.scan.executor

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.executor.BleExecutor
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 中心设备蓝牙扫描执行者。
 */
abstract class AbstractScanExecutor(activity: ComponentActivity) : BleExecutor(
    activity,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 中的新蓝牙权限
        // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
) {
    abstract val scanFlow: Flow<ScanResult>

    /**
     * 开始扫描蓝牙设备，数据从[scanFlow]获取
     *
     * android 7.0 后不能在30秒内扫描和停止超过5次。android 蓝牙模块会打印当前应用扫描太频繁的log日志,并在android 5.0 的ScanCallback回调中触发onScanFailed(int）,返回错误码：ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,表示app无法注册，无法开始扫描）。
     *
     * @param filterServiceUuid     需要过滤的设备服务UUID，默认为null，即不过滤
     * @param duration              扫描时长。如果<=0，表示不限制，则不会自动停止扫描。达到时长时。
     */
    abstract suspend fun startScan(filterServiceUuid: UUID? = null, duration: Long = 10000)

    /**
     * 停止扫描蓝牙设备
     */
    @Throws
    abstract fun stopScan()

}
