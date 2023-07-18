package com.like.ble.central.scan.executor

import android.content.Context
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleExceptionTimeout
import com.like.ble.executor.BleExecutor
import kotlinx.coroutines.flow.Flow

/**
 * 中心设备蓝牙扫描执行者。
 */
abstract class AbstractScanExecutor(context: Context) : BleExecutor(context) {

    /**
     * 开始扫描蓝牙设备
     * 收集此流时，开始执行扫描操作。
     * android 7.0 后不能在30秒内扫描和停止超过5次。android 蓝牙模块会打印当前应用扫描太频繁的log日志,并在android 5.0 的ScanCallback回调中触发onScanFailed(int）,返回错误码：ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,表示app无法注册，无法开始扫描）。
     *
     * @param timeout               扫描持续时长。达到时长后，会自动停止扫描；如果<=0，表示不限制。
     * 当抛出异常[BleExceptionTimeout]代表扫描完成了，即[timeout]到了。
     */
    abstract fun startScan(timeout: Long = 10000L): Flow<ScanResult>

    /**
     * 停止扫描蓝牙设备
     *
     * @throws [com.like.ble.exception.BleException]
     */
    abstract fun stopScan()

}
