package com.like.ble.central.scan.executor

import com.like.ble.executor.IExecutor
import com.like.ble.result.BleResult
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 中心设备蓝牙扫描执行者。
 */
interface IScanExecutor : IExecutor {
    val scanFlow: Flow<BleResult>

    /**
     * 开始扫描蓝牙设备，数据从[scanFlow]获取
     *
     * android 7.0 后不能在30秒内扫描和停止超过5次。android 蓝牙模块会打印当前应用扫描太频繁的log日志,并在android 5.0 的ScanCallback回调中触发onScanFailed(int）,返回错误码：ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,表示app无法注册，无法开始扫描）。
     *
     * @param filterServiceUuid         需要过滤的设备服务UUID，默认为null，即不过滤
     */
    suspend fun startScan(filterServiceUuid: UUID? = null, duration: Long = 10000)

    /**
     * 停止扫描蓝牙设备
     */
    fun stopScan()

}
