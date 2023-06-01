package com.like.ble.central.scan.executor

import android.Manifest
import android.content.Context
import android.os.Build
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.executor.BleExecutor
import kotlinx.coroutines.flow.Flow

/**
 * 中心设备蓝牙扫描执行者。
 */
abstract class AbstractScanExecutor(context: Context) : BleExecutor(context) {

    final override fun getPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
    }

    /**
     * 开始扫描蓝牙设备
     * 收集此流时，开始执行扫描操作。
     * android 7.0 后不能在30秒内扫描和停止超过5次。android 蓝牙模块会打印当前应用扫描太频繁的log日志,并在android 5.0 的ScanCallback回调中触发onScanFailed(int）,返回错误码：ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,表示app无法注册，无法开始扫描）。
     *
     * @param timeout               扫描持续时长。达到时长后，会自动停止扫描；如果<=0，表示不限制。
     */
    abstract fun startScan(timeout: Long = 10000L): Flow<ScanResult>

    /**
     * 停止扫描蓝牙设备
     *
     * @throws [com.like.ble.exception.BleException]
     */
    abstract fun stopScan()

}
