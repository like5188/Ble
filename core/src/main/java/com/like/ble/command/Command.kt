package com.like.ble.command

import androidx.annotation.MainThread
import com.like.ble.executor.ICommandExecutor
import com.like.ble.util.mainThread
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙命令基类
 *
 * @param des           命令功能描述，用于打印日志。
 * @param timeout       命令执行超时时间（毫秒）。<=0表示不设置超时时间。
 * @param immediately   是否立即执行命令。
 * @param onCompleted   命令完成回调
 * @param onError       命令失败回调
 */
abstract class Command(
    val des: String,
    val timeout: Long = 0L,
    val immediately: Boolean = false,
    val onCompleted: (() -> Unit)? = null,
    val onError: ((Throwable) -> Unit)? = null
) {

    /**
     * 命令真正执行者
     */
    var mCommandExecutor: ICommandExecutor? = null

    /**
     * 命令是否已经完成
     */
    private val mIsCompleted: AtomicBoolean = AtomicBoolean(false)

    /**
     * 命令是否出错
     */
    private val mIsError: AtomicBoolean = AtomicBoolean(false)

    /**
     * 异步任务。比如延迟关闭任务等。
     * 在[complete]方法中被关闭。所以逻辑中最终必须要直接或者间接调用[complete]方法来关闭任务。
     */
    private val mJobs = mutableListOf<Job>()

    private var mInterceptor: Interceptor? = null

    fun setInterceptor(interceptor: Interceptor?) {
        mInterceptor = interceptor
    }

    fun isError() = mIsError.get()

    fun isCompleted() = mIsCompleted.get()

    fun addJob(job: Job) {
        mJobs.add(job)
    }

    /**
     * 命令执行完成时调用
     */
    @Synchronized
    fun complete() {
        if (mIsCompleted.compareAndSet(false, true)) {
            clearJobs()
            mainThread {
                mInterceptor?.interceptCompleted(this) ?: onCompleted?.invoke()
            }
        }
    }

    /**
     * 返回结果时调用
     */
    @Synchronized
    fun result(vararg args: Any?) {
        mainThread {
            mInterceptor?.interceptResult(this, *args) ?: onResult(*args)
        }
        if (mIsCompleted.compareAndSet(false, true)) {
            clearJobs()
        }
    }

    /**
     * 错误时调用
     */
    @Synchronized
    fun error(errorMsg: String) {
        if (mIsError.compareAndSet(false, true)) {
            mainThread {
                val t = Throwable(errorMsg)
                mInterceptor?.interceptFailure(this, t) ?: onError?.invoke(t)
            }
            if (mIsCompleted.compareAndSet(false, true)) {
                clearJobs()
            }
        }
    }

    @Synchronized
    fun clearJobs() {
        if (mJobs.isNotEmpty()) {
            mJobs.forEach {
                it.cancel()
            }
            mJobs.clear()
        }
    }

    /**
     * 由子类实现返回参数类型的转换，并调用子类自己特有的回调。
     */
    @MainThread
    open fun onResult(vararg args: Any?) {
    }

    /**
     * 执行命令
     */
    open suspend fun execute() {
        mCommandExecutor?.execute(this)
    }

    override fun toString(): String {
        return "Command(des='$des', isCompleted='${isCompleted()}', isError='${isError()}')"
    }

    /**
     * 回调拦截器，用于组合命令，拦截前提命令的回调。
     */
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

}