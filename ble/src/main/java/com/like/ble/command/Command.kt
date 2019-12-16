package com.like.ble.command

import com.like.ble.state.State
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙命令基类
 *
 * @param des   命令功能描述
 */
abstract class Command(val des: String) {
    /**
     * 命令实际执行者
     */
    internal var mReceiver: State? = null
    /**
     * 命令是否已经完成
     */
    private val mIsCompleted: AtomicBoolean = AtomicBoolean(false)
    /**
     * 命令怎么完成的
     */
    private var mHowCompleted: String = "未完成"
    /**
     * 异步任务。比如延迟关闭命令、执行命令等任务。
     * 在[complete]方法中被关闭。所以逻辑中必须要调用[complete]、[successAndComplete]、[failureAndComplete]、[failureAndComplete]这四个方法之一来关闭任务。
     */
    private val mJobs = mutableListOf<Job>()

    fun addJob(job: Job) {
        mJobs.add(job)
    }

    internal fun isCompleted() = mIsCompleted.get()

    internal fun complete(howCompleted: String) {
        if (isCompleted()) return
        mIsCompleted.set(true)
        mHowCompleted = howCompleted
        if (mJobs.isNotEmpty()) {
            mJobs.forEach {
                it.cancel()
            }
            mJobs.clear()
        }
    }

    /**
     * 命令执行成功时调用，用于扫描蓝牙设备那种多个返回值的情况，最后完成的时候调用[complete]。
     */
    internal fun success(vararg args: Any?) {
        if (isCompleted()) return
        doOnSuccess(*args)
    }

    /**
     * 命令执行成功时调用
     */
    internal fun successAndComplete(vararg args: Any?) {
        if (isCompleted()) return
        doOnSuccess(*args)
        complete("成功完成")
    }

    /**
     * 命令执行失败时调用
     */
    internal fun failureAndComplete(throwable: Throwable) {
        if (isCompleted()) return
        doOnFailure(throwable)
        complete("失败完成：${throwable.message}")
    }

    /**
     * 命令执行失败时调用
     */
    internal fun failureAndComplete(errorMsg: String) {
        if (isCompleted()) return
        doOnFailure(Throwable(errorMsg))
        complete("失败完成：$errorMsg")
    }

    /**
     * 执行命令
     */
    internal abstract fun execute()

    /**
     * 如果命令传入了成功回调方法，则需要重写此方法，在其中回调成功回调方法。
     */
    protected open fun doOnSuccess(vararg args: Any?) {
    }

    /**
     * 如果命令传入了失败回调方法，则需要重写此方法，在其中回调失败回调方法。
     */
    protected open fun doOnFailure(throwable: Throwable) {
    }

    override fun toString(): String {
        return "Command(des='$des', mIsCompleted='${mIsCompleted.get()}', mHowCompleted='$mHowCompleted')"
    }

}