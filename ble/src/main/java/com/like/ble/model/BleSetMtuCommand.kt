package com.like.ble.model

import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.os.Build
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 蓝牙设置MTU的命令
 */
class BleSetMtuCommand(
    private val activity: Activity,
    address: String,
    private val mtu: Int,
    private val onSuccess: ((Int) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : BleCommand(address) {

    override fun setMtu(coroutineScope: CoroutineScope, bluetoothGatt: BluetoothGatt?) {
        if (bluetoothGatt == null) {
            onFailure?.invoke(IllegalArgumentException("设置MTU失败"))
            return
        }

        if (activity !is LifecycleOwner) {
            onFailure?.invoke(IllegalArgumentException("activity 不是 LifecycleOwner"))
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            var observer: Observer<BleResult>? = null
            observer = Observer { bleResult ->
                if (bleResult?.status == BleStatus.ON_MTU_CHANGED_SUCCESS) {
                    removeObserver(observer)
                    onSuccess?.invoke(mtu)
                } else if (bleResult?.status == BleStatus.ON_MTU_CHANGED_FAILURE) {
                    removeObserver(observer)
                    onFailure?.invoke(RuntimeException("设置MTU失败"))
                }
            }

            withContext(Dispatchers.Main) {
                mLiveData?.value = null// 避免残留值影响下次命令
                mLiveData?.observe(activity, observer)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothGatt.requestMtu(mtu)
            } else {
                onFailure?.invoke(RuntimeException("android 5.0 才支持 setMtu() 操作"))
            }
        }
    }

    private fun removeObserver(observer: Observer<BleResult>?) {
        observer ?: return
        activity.runOnUiThread {
            mLiveData?.removeObserver(observer)
        }
    }
}


