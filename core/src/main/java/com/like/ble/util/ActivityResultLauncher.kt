package com.like.ble.util

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

open class BaseActivityResultLauncher<I, O>(
    private val registry: ActivityResultRegistry,
    private val contract: ActivityResultContract<I, O>
) {

    suspend fun launch(input: I, options: ActivityOptionsCompat? = null): O =
        suspendCancellableCoroutine { continuation ->
            launch(input, options) {
                continuation.resume(it)
            }
        }

    fun launch(input: I, options: ActivityOptionsCompat? = null, callback: ActivityResultCallback<O>) {
        val key = "com.like.ble.util.BaseActivityResultLauncher.${hashCode()}"
        var launcher: ActivityResultLauncher<I>? = null
        // 除了带有 LifecycleOwner 参数的 register 函数（此函数如果使用位置不当，会报错：LifecycleOwners must call register before they are STARTED）外，
        // 还有一个不带 LifecycleOwner 的函数，这里就使用了它，它内部没有对生命周期相关的判断，
        // 所以需要我们自己管理 unregister 的调用，这里在返回结果时调用 unregister。
        // 所以也就不支持在 Launcher 的重复使用，每次都会去创建一个新的 Launcher。
        launcher = registry.register(key, contract) { result ->
            callback.onActivityResult(result)
            launcher?.unregister()
        }.apply {
            launch(input, options)
        }
    }

}

class RequestMultiplePermissionsLauncher(registry: ActivityResultRegistry) :
    BaseActivityResultLauncher<Array<String>, Map<String, Boolean>>(
        registry, ActivityResultContracts.RequestMultiplePermissions()
    )

suspend fun ComponentActivity.requestMultiplePermissions(
    vararg permissions: String,
    options: ActivityOptionsCompat? = null
): Map<String, Boolean> {
    return RequestMultiplePermissionsLauncher(activityResultRegistry).launch(arrayOf(*permissions), options)
}

class StartActivityForResultLauncher(registry: ActivityResultRegistry) :
    BaseActivityResultLauncher<Intent, ActivityResult>(
        registry, ActivityResultContracts.StartActivityForResult()
    )

suspend fun ComponentActivity.startActivityForResult(
    intent: Intent,
    options: ActivityOptionsCompat? = null
): ActivityResult {
    return StartActivityForResultLauncher(activityResultRegistry).launch(intent, options)
}
