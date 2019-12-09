package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.like.ble.model.BleResult
import com.like.ble.model.BleStatus
import com.like.ble.scanstrategy.IScanStrategy
import com.like.ble.utils.getBluetoothAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙扫描状态
 * 可以进行扫描操作
 */
class ScanState(
    activity: FragmentActivity,
    bleResultLiveData: MutableLiveData<BleResult>
) : BaseBleState(activity, bleResultLiveData) {
    private val mScanning = AtomicBoolean(false)
    private var mScanStrategy: IScanStrategy? = null

    override fun onStartScan(scanStrategy: IScanStrategy, scanTimeout: Long) {
        mScanStrategy = scanStrategy
        if (mScanning.compareAndSet(false, true)) {
            mBleResultLiveData.postValue(BleResult(BleStatus.START_SCAN_DEVICE))
            scanStrategy.startScan(mActivity.getBluetoothAdapter())
            mActivity.lifecycleScope.launch(Dispatchers.IO) {
                // 在指定超时时间时取消扫描
                delay(scanTimeout)
                if (mScanning.get()) {
                    stopScan()
                }
            }
        }
    }

    override fun onStopScan() {
        if (mScanning.compareAndSet(true, false)) {
            mBleResultLiveData.postValue(BleResult(BleStatus.STOP_SCAN_DEVICE))
            mScanStrategy?.stopScan(mActivity.getBluetoothAdapter())
        }
    }

    override fun onClose() {
        stopScan()
        mScanStrategy = null
    }

}