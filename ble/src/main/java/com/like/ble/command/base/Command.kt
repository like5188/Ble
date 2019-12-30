package com.like.ble.command.base

import androidx.annotation.MainThread
import com.like.ble.state.State
import com.like.ble.utils.mainThread
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙命令基类
 *
 * @param des           命令功能描述
 * @param timeout       命令执行超时时间（毫秒）。0或者小于0表示没有超时时间。
 * @param callback      命令回调
 */
abstract class Command(
    private val des: String,
    val timeout: Long = 0L,
    val callback: Callback? = null
) {

    /**
     * 命令实际执行者
     */
    internal var mReceiver: State? = null
    /**
     * 命令是否已经完成
     */
    private val mIsCompleted: AtomicBoolean = AtomicBoolean(false)

    /**
     * 命令是否成功
     */
    private val mIsSuccess: AtomicBoolean = AtomicBoolean(false)

    /**
     * 异步任务。比如延迟关闭任务、执行任务等。
     * 在[complete]方法中被关闭。所以逻辑中最终必须要直接或者间接调用[complete]方法来关闭任务。
     */
    private val mJobs = mutableListOf<Job>()

    private var mInterceptor: Interceptor? = null

    internal fun addInterceptor(interceptor: Interceptor?) {
        mInterceptor = interceptor
    }

    internal fun isSuccess() = mIsSuccess.get()

    internal fun isCompleted() = mIsCompleted.get()

    fun addJob(job: Job) {
        mJobs.add(job)
    }

    internal fun complete() {
        mIsCompleted.set(true)
        if (mJobs.isNotEmpty()) {
            mJobs.forEach {
                it.cancel()
            }
            mJobs.clear()
        }
        mainThread {
            mInterceptor?.interceptCompleted(this) ?: callback?.onCompleted()
        }
    }

    internal fun resultAndComplete(vararg args: Any?) {
        mIsSuccess.set(true)
        mainThread {
            mInterceptor?.interceptResult(this, *args) ?: callback?.onResult(*args)
        }
        complete()
    }

    internal fun failureAndComplete(errorMsg: String) {
        mIsSuccess.set(false)
        mainThread {
            val t = Throwable(errorMsg)
            mInterceptor?.interceptFailure(this, t) ?: callback?.onFailure(t)
        }
        complete()
    }

    internal fun resultAndCompleteIfIncomplete(vararg args: Any?) {
        if (isCompleted()) return
        resultAndComplete(*args)
    }

    internal fun failureAndCompleteIfIncomplete(errorMsg: String) {
        if (isCompleted()) return
        failureAndComplete(errorMsg)
    }

    /**
     * 执行命令
     */
    internal abstract suspend fun execute()

    override fun toString(): String {
        return "Command(des='$des', isCompleted='${isCompleted()}', isSuccess='${isSuccess()}')"
    }

    interface Interceptor {
        @MainThread
        fun interceptCompleted(command: Command)

        /**
         * 如果命令传入了失败回调方法，则需要重写此方法，在其中回调失败回调方法。
         */
        @MainThread
        fun interceptFailure(command: Command, throwable: Throwable)

        /**
         * 如果命令传入了成功回调方法，则需要重写此方法，在其中回调成功回调方法。
         */
        @MainThread
        fun interceptResult(command: Command, vararg args: Any?)
    }

    abstract class Callback {
        open fun onCompleted() {}
        open fun onFailure(t: Throwable) {}
        open fun onResult(vararg args: Any?) {}
    }
}