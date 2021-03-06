package com.like.ble.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Size
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tbruyelle.rxpermissions2.RxPermissions

/**
 * 运行时动态请求危险权限。
 * <p>
 * 注意：<br/>
 * ① Android 6.0 ~ Android 8.0同一组权限中，申请了其中一个，则此组中所有权限都申请了。在 Android 8.0 之后，此行为已被纠正。系统只会授予应用明确请求的权限<br/>
 * 在 Android 8.0 之前，如果应用在运行时请求权限并且被授予该权限，系统会错误地将属于同一权限组并且在清单中注册的其他权限也一起授予应用。
 * 对于针对 Android 8.0 的应用，此行为已被纠正。系统只会授予应用明确请求的权限。然而，一旦用户为应用授予某个权限，则所有后续对该权限组中权限的请求都将被自动批准。
 * 例如，假设某个应用在其清单中列出 READ_EXTERNAL_STORAGE 和 WRITE_EXTERNAL_STORAGE。应用请求 READ_EXTERNAL_STORAGE，
 * 并且用户授予了该权限。如果该应用针对的是 API 级别 24 或更低级别，系统还会同时授予 WRITE_EXTERNAL_STORAGE，
 * 因为该权限也属于同一 STORAGE 权限组并且也在清单中注册过。如果该应用针对的是 Android 8.0，
 * 则系统此时仅会授予 READ_EXTERNAL_STORAGE；不过，如果该应用后来又请求 WRITE_EXTERNAL_STORAGE，则系统会立即授予该权限，而不会提示用户。
 *
 * ②申请的危险权限必须在AndroidManifest.xml中申明，否则不会弹出系统权限授权的对话框。<br/>
 */
class PermissionUtils {
    private val mContext: Context?
    private val mRxPermissions: RxPermissions

    constructor(fragmentActivity: FragmentActivity) {
        mContext = fragmentActivity.applicationContext
        mRxPermissions = RxPermissions(fragmentActivity)
    }

    constructor(fragment: Fragment) {
        mContext = fragment.context?.applicationContext
        mRxPermissions = RxPermissions(fragment)
    }

    /**
     * 检查权限并执行代码
     *
     * @param perms             需要申请的所有权限
     * @param onDenied          权限被拒绝，然后需要执行的代码
     * @param onError           出错后需要执行的代码
     * @param onGranted         权限通过，然后需要执行的代码
     */
    internal fun checkPermissions(
        @Size(min = 1) vararg perms: String,
        onDenied: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        onGranted: (() -> Unit)
    ) {
        when {
            mContext == null -> {
                onError?.invoke(UnsupportedOperationException("context is null"))
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {// 6.0 及其以上
                if (hasPermissions(mContext, *perms)) {
                    // 如果是普通权限：说明在 AndroidManifest.xml 中声明了。
                    // 如果是危险权限：说明在 AndroidManifest.xml 中声明，并且授予了权限。
                    onGranted.invoke()
                } else {
                    // 如果是普通权限：说明在 AndroidManifest.xml 中没有声明，调用 request() 方法都会返回 false。
                    // 如果是危险权限：如果在 AndroidManifest.xml 中没有声明，调用 request() 方法都会返回 false；如果声明了，调用该方法会弹出权限授权框。
                    mRxPermissions.request(*perms).subscribe(
                        {
                            if (it) {
                                onGranted.invoke()
                            } else {
                                onDenied?.invoke()
                            }
                        },
                        {
                            onError?.invoke(it)
                        }
                    )
                }
            }
            else -> {// 6.0 以下
                if (hasPermissions(mContext, *perms)) {// 在 AndroidManifest.xml 中声明了。
                    onGranted.invoke()
                } else {// 在 AndroidManifest.xml 中没有声明。
                    onDenied?.invoke()
                }
            }
        }
    }

    /**
     * 检查是否拥有指定权限。
     * 普通权限：只是检查是否在 AndroidManifest.xml 中声明了权限。
     * 危险权限：
     *      ①、6.0 以下：只是检查是否在 AndroidManifest.xml 中声明了权限。
     *      ②、6.0 以上：除了要检查是否在 AndroidManifest.xml 中声明了权限外，还检查是否授予了权限。
     */
    private fun hasPermissions(context: Context, @Size(min = 1) vararg perms: String): Boolean {
        for (perm in perms) {
            if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

}