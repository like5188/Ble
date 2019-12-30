package com.like.ble.command.base

import com.like.ble.state.State
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙命令基类
 *
 * @param des       命令功能描述
 * @param timeout   命令执行超时时间（毫秒）。0或者小于0表示没有超时时间。
 */
abstract class Command(private val des: String, val timeout: Long = 0L) {

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
     * 执行命令
     */
    internal abstract suspend fun execute()

    override fun toString(): String {
        return "Command(des='$des', isCompleted='${isCompleted()}')"
    }

}