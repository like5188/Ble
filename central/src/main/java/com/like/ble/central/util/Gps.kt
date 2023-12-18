package com.like.ble.central.util

import android.content.Context
import android.location.LocationManager

private val Context.locationManager: LocationManager get() = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

/**
 * 检查是否有 GPS 模块
 *
 */
fun Context.hasGps(): Boolean = locationManager.allProviders.any {
    it == LocationManager.GPS_PROVIDER
}

/**
 * 检查 GPS 是否打开
 *
 */
fun Context.isGPSOpen(): Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
