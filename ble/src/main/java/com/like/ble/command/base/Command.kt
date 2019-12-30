package com.like.ble.command.base

import androidx.annotation.MainThread
import com.like.ble.state.State
import com.like.ble.utils.mainThread
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙命令基类
 *
 * @param des       命令功能描述
 * @param timeout   命令执行超时时间（毫秒）。默认0L，表示没有超时时间。
 */
abstract class Command(private val des: String, val timeout: Long = 0L) {
    init {
        if (timeout < 0L) {
            failureAndCompleteIfIncomplete("timeout must be greater than or equal to 0")
        }
    }

    /**
     * 命令实际执行者
     */
    internal var mReceiver: State? = null
    /**
     * 命令是否已经完成
     */
    private val mIsCompleted: AtomicBoolean = AtomicBoolean(false)
    /**
     * 异步任务。比如延迟关闭任务、执行任务等。
     * 在[complete]方法中被关闭。所以逻辑中最终必须要直接或者间接调用[complete]方法来关闭任务。
     */
    private val mJobs = mutableListOf<Job>()

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
    }

    /**
     * 命令执行成功时调用
     */
    internal fun successAndCompleteIfIncomplete(vararg args: Any?) {
        if (isCompleted()) return
        successAndComplete(*args)
    }

    /**
     * 命令执行失败时调用
     */
    internal fun failureAndCompleteIfIncomplete(errorMsg: String) {
        if (isCompleted()) return
        failureAndComplete(errorMsg)
    }

    /**
     * 在命令完成后也可以继续触发 [doOnSuccess] 回调。
     * 用于长连接的命令，比如[com.like.ble.command.StartAdvertisingCommand]、[com.like.ble.command.StartScanCommand]、[com.like.ble.command.ConnectCommand]。
     */
    internal fun successAndComplete(vararg args: Any?) {
        mainThread {
            doOnSuccess(*args)
        }
        complete()
    }

    /**
     * 在命令完成后也可以继续触发 [doOnFailure] 回调。
     * 用于长连接的命令，比如[com.like.ble.command.StartAdvertisingCommand]、[com.like.ble.command.StartScanCommand]、[com.like.ble.command.ConnectCommand]。
     */
    internal fun failureAndComplete(errorMsg: String) {
        mainThread {
            doOnFailure(Throwable(errorMsg))
        }
        complete()
    }

    /**
     * 如果命令传入了成功回调方法，则需要重写此方法，在其中回调成功回调方法。
     */
    @MainThread
    protected open fun doOnSuccess(vararg args: Any?) {
    }

    /**
     * 如果命令传入了失败回调方法，则需要重写此方法，在其中回调失败回调方法。
     */
    @MainThread
    protected open fun doOnFailure(throwable: Throwable) {
    }

    /**
     * 执行命令
     */
    internal abstract suspend fun execute()

    override fun toString(): String {
        return "Command(des='$des', mIsCompleted='${isCompleted()}')"
    }

}