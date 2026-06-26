package cn.chahuyun.economy.scheduler

import java.util.concurrent.ScheduledFuture

/**
 * 定时任务句柄。
 *
 * 两种引擎共用同一接口，内部实现不同：
 * - [ExecutorScheduledTask] 用于 [ScheduledExecutorEngine]
 * - 后期 CoroutineEngine 将提供协程版实现
 *
 * @author Moyuyanli
 * @since 2.0.0
 */
interface ScheduledTask {

    /** 任务唯一标识 */
    val id: String

    /** cron 表达式（仅 cron 任务有值） */
    val cron: String?

    /**
     * 取消任务。
     * - 如果任务正在等待中，会立即中断等待
     * - 如果任务正在执行中，会设置中断标志
     */
    fun cancel()

    /** 任务是否已被取消 */
    fun isCancelled(): Boolean

    /** 任务是否已完成（包括被取消） */
    fun isDone(): Boolean
}

/**
 * 基于 [ScheduledFuture] 的任务句柄实现。
 *
 * @param future      ScheduledFuture 引用
 * @param timerThread cron 任务的等待线程（仅 cron 任务有值），可被 interrupt 瞬间唤醒
 */
internal class ExecutorScheduledTask(
    override val id: String,
    override val cron: String? = null,
    @Volatile private var future: ScheduledFuture<*>,
    private val timerThread: Thread? = null
) : ScheduledTask {

    override fun cancel() {
        future.cancel(true)      // 中断执行中的任务
        timerThread?.interrupt() // 中断 cron 等待线程（双保险）
    }

    override fun isCancelled(): Boolean = future.isCancelled

    override fun isDone(): Boolean = future.isDone

    /**
     * 更新底层 Future 引用（用于 cron 任务自调度时替换旧 Future）。
     */
    internal fun updateFuture(newFuture: ScheduledFuture<*>) {
        this.future = newFuture
    }

    override fun toString(): String = "ScheduledTask(id=$id, cron=$cron, cancelled=${isCancelled()}, done=${isDone()})"
}
