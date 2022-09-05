package com.like.ble.command

import androidx.annotation.MainThread
import com.like.ble.state.IState
import com.like.ble.utils.mainThread
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙命令基类
 *
 * @param des           命令功能描述，用于打印日志。
 * @param timeout       命令执行超时时间（毫秒）。<=0表示立即执行。
 * @param onCompleted   命令完成回调
 * @param onError       命令失败回调
 */
abstract class Command(
    private val des: String,
    val timeout: Long = 0L,
    val onCompleted: (() -> Unit)? = null,
    val onError: ((Throwable) -> Unit)? = null
) {

    /**
     * 命令实际执行者
     */
    var mState: IState? = null

    /**
     * 命令是否已经完成
     */
    private val mIsCompleted: AtomicBoolean = AtomicBoolean(false)

    /**
     * 命令是否出错
     */
    private val mIsError: AtomicBoolean = AtomicBoolean(false)

    /**
     * 异步任务。比如延迟关闭任务、执行任务等。
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
     * 命令执行完成时回调
     */
    @Synchronized
    fun complete() {
        if (mIsCompleted.compareAndSet(false, true)) {
            if (mJobs.isNotEmpty()) {
                mJobs.forEach {
                    it.cancel()
                }
                mJobs.clear()
            }
            mainThread {
                mInterceptor?.interceptCompleted(this) ?: onCompleted?.invoke()
            }
        }
    }

    /**
     * 返回结果时回调
     */
    @Synchronized
    fun resultAndComplete(vararg args: Any?) {
        mainThread {
            mInterceptor?.interceptResult(this, *args) ?: doOnResult(*args)
        }
        complete()
    }

    /**
     * 错误时回调
     */
    @Synchronized
    fun errorAndComplete(errorMsg: String) {
        if (mIsError.compareAndSet(false, true)) {
            mainThread {
                val t = Throwable(errorMsg)
                mInterceptor?.interceptFailure(this, t) ?: onError?.invoke(t)
            }
            complete()
        }
    }

    /**
     * 由子类实现返回参数类型的转换
     */
    @MainThread
    open fun doOnResult(vararg args: Any?) {
    }

    /**
     * 执行命令
     */
    open suspend fun execute() {
        mState?.execute(this)
    }

    /**
     * 是否属于需要立即执行的命令
     */
    abstract fun needExecuteImmediately(): Boolean

    override fun toString(): String {
        return "Command(des='$des', isCompleted='${isCompleted()}', isError='${isError()}')"
    }

    /**
     * 回调拦截器，用于宏命令[com.like.ble.command.MacroCommand]，拦截前提命令的回调。
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