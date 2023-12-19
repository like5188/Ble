package com.like.ble.central.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.like.ble.util.startActivityForResult

private val Context.locationManager: LocationManager get() = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

/**
 * 检查是否有 gps 模块
 *
 */
fun Context.hasGps(): Boolean = locationManager.allProviders.any {
    it == LocationManager.GPS_PROVIDER
}

/**
 * 检查 gps 是否打开。
 * 如果没有 gps 模块返回 true
 *
 */
fun Context.isGpsOpen(): Boolean = if (hasGps()) {
    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
} else {
    true
}

/**
 * gps 是否打开，如果没打开，就去打开。
 */
suspend fun ComponentActivity.isGpsOpenAndSettingIfClosed(): Boolean = if (!isGpsOpen()) {
    startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)).resultCode == Activity.RESULT_OK
} else {
    true
}
