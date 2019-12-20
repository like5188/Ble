package com.like.ble.command

import com.like.ble.state.State
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙命令基类
 *
 * @param des   命令功能描述
 */
abstract class Command(val des: String, val address: String = "") {
    companion object {
        // 命令分组，用于过滤判断条件等
        /**
         * 关闭命令
         */
        internal const val GROUP_CLOSE = 1 shl 0
        /**
         * 与外围设备相关的命令
         */
        internal const val GROUP_PERIPHERAL = 1 shl 1
        /**
         * 与外围设备广播相关的命令
         */
        internal const val GROUP_PERIPHERAL_ADVERTISING = 1 shl 2
        /**
         * 与中心设备相关的命令
         */
        internal const val GROUP_CENTRAL = 1 shl 3
        /**
         * 与中心设备扫描相关的命令
         */
        internal const val GROUP_CENTRAL_SCAN = 1 shl 4
        /**
         * 与具体中心设备相关的命令。即含有address字段的命令。
         */
        internal const val GROUP_CENTRAL_DEVICE = 1 shl 5
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

    private fun complete() {
        mIsCompleted.set(true)
        if (mJobs.isNotEmpty()) {
            mJobs.forEach {
                it.cancel()
            }
            mJobs.clear()
        }
    }

    fun addJob(job: Job) {
        mJobs.add(job)
    }

    internal fun completeIfIncomplete() {
        if (isCompleted()) return
        complete()
    }

    /**
     * 命令执行成功时调用，不会设置完成标志，用于[StartScanCommand]，因为需要多次返回值，最后完成的时候调用[completeIfIncomplete]。
     */
    internal fun successIfIncomplete(vararg args: Any?) {
        if (isCompleted()) return
        doOnSuccess(*args)
    }

    /**
     * 命令执行成功时调用
     */
    internal fun successAndCompleteIfIncomplete(vararg args: Any?) {
        if (isCompleted()) return
        doOnSuccess(*args)
        complete()
    }

    /**
     * 命令执行失败时调用
     */
    internal fun failureAndCompleteIfIncomplete(throwable: Throwable) {
        if (isCompleted()) return
        doOnFailure(throwable)
        complete()
    }

    /**
     * 命令执行失败时调用
     */
    internal fun failureAndCompleteIfIncomplete(errorMsg: String) {
        if (isCompleted()) return
        doOnFailure(Throwable(errorMsg))
        complete()
    }

    /**
     * 这个方法可以多次触发[doOnFailure]回调，不会判断是否完成。用于[ConnectCommand]、[StartAdvertisingCommand]，在连接成功后，连接断开时通知更新界面。
     */
    internal fun failureAndComplete(errorMsg: String) {
        doOnFailure(Throwable(errorMsg))
        complete()
    }

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

    /**
     * 执行命令
     */
    internal abstract fun execute()

    /**
     * @return 命令所属分组[GROUP_CLOSE]、[GROUP_PERIPHERAL]、[GROUP_PERIPHERAL_ADVERTISING]、[GROUP_CENTRAL]、[GROUP_CENTRAL_SCAN]、[GROUP_CENTRAL_DEVICE]
     */
    protected abstract fun getGroups(): Int

    /**
     * @param group  需要判断的分组值
     * @return 是否存在
     */
    fun hasGroup(group: Int): Boolean {
        return getGroups() and group != 0
    }

    override fun toString(): String {
        return "Command(des='$des', address='$address', mIsCompleted='${mIsCompleted.get()}')"
    }

}