package cn.chahuyun.economy.scheduler

import cn.chahuyun.economy.utils.Log
import cn.hutool.cron.pattern.CronPattern
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 基于 [ScheduledExecutorService] 的调度引擎实现。
 *
 * - 所有线程均为 daemon 线程，不阻塞 JVM 关闭
 * - cron 任务采用"自调度"模式：执行完毕后计算下次时间并重新提交
 * - [shutdown] 瞬间中断所有任务：先 cancel(true) 所有 future，再 shutdownNow()
 *
 * @author Moyuyanli
 * @since 2.0.0
 */
internal class ScheduledExecutorEngine : SchedulerEngine {

    companion object {
        private val threadCounter = AtomicInteger(0)
    }

    private val running = AtomicBoolean(false)
    private val tasks = ConcurrentHashMap<String, ScheduledTask>()

    private val executor: ScheduledExecutorService by lazy {
        val corePoolSize = Runtime.getRuntime().availableProcessors().coerceAtMost(4)
        Executors.newScheduledThreadPool(corePoolSize) { r ->
            Thread(r, "HuYan-Scheduler-${threadCounter.incrementAndGet()}").apply {
                isDaemon = true
            }
        }
    }

    // ───────────── 生命周期 ─────────────

    override fun start() {
        if (running.compareAndSet(false, true)) {
            // 触发 lazy 初始化
            executor
            Log.info("HuYanScheduler 已启动")
        }
    }

    override fun shutdown() {
        if (running.compareAndSet(true, false)) {
            // 1. 取消所有已注册任务（中断执行中的 + 取消等待中的）
            for ((id, task) in tasks) {
                try {
                    task.cancel()
                } catch (e: Exception) {
                    Log.warning("HuYanScheduler: 取消任务 $id 异常")
                }
            }
            tasks.clear()

            // 2. 关闭线程池（interrupt 所有工作线程）
            executor.shutdownNow()

            Log.info("HuYanScheduler 已停止，所有任务已取消")
        }
    }

    override fun isRunning(): Boolean = running.get()

    // ───────────── Cron 调度 ─────────────

    override fun scheduleCron(id: String, cron: String, task: Runnable): ScheduledTask {
        cancel(id)

        val cronPattern = CronPattern(cron)
        val scheduledTask = ExecutorScheduledTask(id, cron, NoOpFuture)
        tasks[id] = scheduledTask

        val selfScheduling = Runnable {
            if (!running.get() || Thread.currentThread().isInterrupted) return@Runnable
            try {
                task.run()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return@Runnable
            } catch (e: Exception) {
                Log.error("HuYanScheduler: 任务 $id 执行异常", e)
            }
            if (!running.get() || Thread.currentThread().isInterrupted) return@Runnable
            // 自调度：计算下次时间并重新提交
            rescheduleCron(id, cron, cronPattern, task, scheduledTask)
        }

        val now = System.currentTimeMillis()
        val nextTime = cronPattern.nextMatchAfter(Calendar.getInstance())
        val initialDelay = (nextTime.timeInMillis - now).coerceAtLeast(0)

        val future = executor.schedule(selfScheduling, initialDelay, TimeUnit.MILLISECONDS)
        scheduledTask.updateFuture(future)

        Log.info("HuYanScheduler: 注册 cron 任务 [$id] expr='$cron'")
        return scheduledTask
    }

    private fun rescheduleCron(
        id: String,
        cron: String,
        cronPattern: CronPattern,
        task: Runnable,
        scheduledTask: ExecutorScheduledTask
    ) {
        if (!running.get()) return

        val selfScheduling = Runnable {
            if (!running.get() || Thread.currentThread().isInterrupted) return@Runnable
            try {
                task.run()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return@Runnable
            } catch (e: Exception) {
                Log.error("HuYanScheduler: 任务 $id 执行异常", e)
            }
            if (!running.get() || Thread.currentThread().isInterrupted) return@Runnable
            rescheduleCron(id, cron, cronPattern, task, scheduledTask)
        }

        try {
            val now = System.currentTimeMillis()
            val nextTime = cronPattern.nextMatchAfter(Calendar.getInstance())
            val delay = (nextTime.timeInMillis - now).coerceAtLeast(0)
            val future = executor.schedule(selfScheduling, delay, TimeUnit.MILLISECONDS)
            scheduledTask.updateFuture(future)
        } catch (e: RejectedExecutionException) {
            // 线程池已关闭，忽略
        }
    }

    // ───────────── 固定频率 / 固定延迟 / 一次性 ─────────────

    override fun scheduleFixedRate(
        id: String,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit,
        task: Runnable
    ): ScheduledTask {
        cancel(id)

        val wrappedTask = wrapTask(id, task)
        val future = executor.scheduleAtFixedRate(wrappedTask, initialDelay, period, unit)
        val scheduledTask = ExecutorScheduledTask(id, null, future)
        tasks[id] = scheduledTask

        Log.info("HuYanScheduler: 注册 fixed-rate 任务 [$id] period=${period}${unit.name.lowercase()}")
        return scheduledTask
    }

    override fun scheduleFixedDelay(
        id: String,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit,
        task: Runnable
    ): ScheduledTask {
        cancel(id)

        val wrappedTask = wrapTask(id, task)
        val future = executor.scheduleWithFixedDelay(wrappedTask, initialDelay, delay, unit)
        val scheduledTask = ExecutorScheduledTask(id, null, future)
        tasks[id] = scheduledTask

        Log.info("HuYanScheduler: 注册 fixed-delay 任务 [$id] delay=${delay}${unit.name.lowercase()}")
        return scheduledTask
    }

    override fun scheduleOnce(id: String, delay: Long, unit: TimeUnit, task: Runnable): ScheduledTask {
        cancel(id)

        val wrappedTask = wrapTask(id, task)
        val future = executor.schedule(wrappedTask, delay, unit)
        val scheduledTask = ExecutorScheduledTask(id, null, future)
        tasks[id] = scheduledTask

        return scheduledTask
    }

    // ───────────── 取消 ─────────────

    override fun cancel(id: String) {
        val task = tasks.remove(id)
        if (task != null) {
            try {
                task.cancel()
            } catch (e: Exception) {
                Log.warning("HuYanScheduler: 取消任务 $id 异常")
            }
        }
    }

    override fun taskIds(): Set<String> = tasks.keys.toSet()

    // ───────────── 内部工具 ─────────────

    /**
     * 包装任务：捕获异常，检查中断标志。
     */
    private fun wrapTask(id: String, task: Runnable): Runnable = Runnable {
        if (Thread.currentThread().isInterrupted) return@Runnable
        try {
            task.run()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            Log.error("HuYanScheduler: 任务 $id 执行异常", e)
        }
    }

    /**
     * 空操作 Future，用于 cron 任务初始化时的占位。
     */
    private object NoOpFuture : ScheduledFuture<Any?> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = true
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = false
        override fun get(): Any? = null
        override fun get(timeout: Long, unit: TimeUnit): Any? = null
        override fun getDelay(unit: TimeUnit): Long = Long.MAX_VALUE
        override fun compareTo(other: Delayed?): Int = 0
    }
}
