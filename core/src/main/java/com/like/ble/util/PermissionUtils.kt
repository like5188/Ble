package com.like.ble.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

/**
 * 检查是否拥有指定的权限
 */
fun Context.checkPermissions(vararg permissions: String): Boolean {
    if (permissions.isEmpty()) {
        return true
    }
    permissions.forEach {
        if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
    }
    return true
}

/**
 * 检查是否拥有指定的权限，没有就去请求。
 * @return true：同意了所有权限；false：没有同意所有权限；
 */
suspend fun ComponentActivity.requestPermissions(vararg permissions: String): Boolean {
    if (permissions.isEmpty()) {
        return true
    }
    return requestMultiplePermissions(*permissions).all { it.value }
}
