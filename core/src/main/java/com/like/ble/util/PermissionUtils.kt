package com.like.ble.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

object PermissionUtils {

    /**
     * 检查是否拥有指定的权限
     */
    fun checkPermissions(context: Context, vararg permissions: String): Boolean {
        if (permissions.isEmpty()) {
            return true
        }
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * 检查并请求指定的权限
     * @return true：同意了所有权限；false：没有同意所有权限；
     */
    suspend fun requestPermissions(activity: ComponentActivity, vararg permissions: String): Boolean {
        if (permissions.isEmpty()) {
            return true
        }
        return activity.requestMultiplePermissions(*permissions).all { it.value }
    }

}
