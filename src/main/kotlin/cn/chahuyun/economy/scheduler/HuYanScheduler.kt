package cn.chahuyun.economy.scheduler

import cn.chahuyun.economy.scheduler.HuYanScheduler.stop
import cn.chahuyun.economy.scheduler.HuYanScheduler.switchEngine
import cn.chahuyun.economy.utils.Log
import java.util.concurrent.TimeUnit

/**
 * 壶言经济统一调度器门面。
 *
 * 替代 Hutool [cn.hutool.cron.CronUtil]，提供：
 * - **cron 定时** — 支持 Hutool cron 表达式（含 `?` 通配符）
 * - **固定频率** — fixed-rate，按固定间隔执行
 * - **固定延迟** — fixed-delay，上一次结束到下一次开始的延迟
 * - **一次性延迟** — 延迟指定时间后执行一次
 *
 * 瞬间停止能力：[stop] 会立即中断所有任务（含正在执行中的），不等待超时。
 *
 * 后期迁移路径：通过 [switchEngine] 可将底层引擎从 [ScheduledExecutorEngine] 切换为
 * CoroutineEngine（基于 kotlinx.coroutines delay），门面 API 保持不变。
 *
 * @author Moyuyanli
 * @since 2.0.0
 */
object HuYanScheduler {

    @Volatile
    private var engine: SchedulerEngine = ScheduledExecutorEngine()
    private val lock = Any()
    private val submittedTasks = linkedMapOf<String, SubmittedTask>()

    @Volatile
    private var submittedTasksRegistered = false

    @Volatile
    private var acceptingTasks = true

    // ───────────── 生命周期 ─────────────

    /**
     * 启动调度器。重复调用安全。
     */
    @JvmStatic
    fun start() = engine.start()

    @JvmStatic
    fun prepareStartup() {
        synchronized(lock) {
            submittedTasks.clear()
            submittedTasksRegistered = false
            acceptingTasks = true
        }
    }

    /**
     * 瞬间停止调度器，取消所有任务。
     * 不等待正在执行的任务完成。
     */
    @JvmStatic
    fun stop() {
        val stoppedEngine = engine
        synchronized(lock) {
            acceptingTasks = false
            submittedTasks.values.forEach { it.handle.cancel() }
            submittedTasks.clear()
            submittedTasksRegistered = false
        }
        stoppedEngine.shutdown()
        synchronized(lock) {
            if (engine === stoppedEngine && stoppedEngine is ScheduledExecutorEngine) {
                engine = ScheduledExecutorEngine()
            }
        }
    }

    /**
     * 调度器是否正在运行。
     */
    @JvmStatic
    fun isRunning(): Boolean = engine.isRunning()

    // ───────────── cron 调度 ─────────────

    /**
     * 注册 cron 定时任务。
     *
     * @param id   任务唯一标识，重复 id 会先取消旧任务再注册新任务
     * @param cron cron 表达式（Hutool 语法，如 `"0 0 4 * * ?"`）
     * @param task 要执行的任务
     * @return 任务句柄，可用于取消
     */
    @JvmStatic
    fun schedule(id: String, cron: String, task: Runnable): ScheduledTask {
        return submitOrRegister(SubmittedTask.Cron(id, cron, task))
    }

    // ───────────── 固定频率 ─────────────

    /**
     * 注册固定频率任务（fixed-rate）。
     *
     * @param id           任务唯一标识
     * @param initialDelay 首次执行延迟
     * @param period       执行周期
     * @param unit         时间单位
     * @param task         要执行的任务
     * @return 任务句柄
     */
    @JvmStatic
    fun scheduleAtFixedRate(
        id: String,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit,
        task: Runnable
    ): ScheduledTask {
        return submitOrRegister(SubmittedTask.FixedRate(id, initialDelay, period, unit, task))
    }

    // ───────────── 固定延迟 ─────────────

    /**
     * 注册固定延迟任务（fixed-delay，上一次结束到下一次开始之间的延迟）。
     *
     * @param id           任务唯一标识
     * @param initialDelay 首次执行延迟
     * @param delay        两次执行之间的延迟
     * @param unit         时间单位
     * @param task         要执行的任务
     * @return 任务句柄
     */
    @JvmStatic
    fun scheduleWithFixedDelay(
        id: String,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit,
        task: Runnable
    ): ScheduledTask {
        return submitOrRegister(SubmittedTask.FixedDelay(id, initialDelay, delay, unit, task))
    }

    // ───────────── 一次性延迟 ─────────────

    /**
     * 注册一次性延迟任务。
     *
     * @param id    任务唯一标识
     * @param delay 延迟时间
     * @param unit  时间单位
     * @param task  要执行的任务
     * @return 任务句柄
     */
    @JvmStatic
    fun scheduleOnce(id: String, delay: Long, unit: TimeUnit, task: Runnable): ScheduledTask {
        return submitOrRegister(SubmittedTask.Once(id, delay, unit, task))
    }

    // ───────────── 取消 ─────────────

    /**
     * 取消指定任务。如果任务不存在，静默忽略。
     *
     * @param id 任务唯一标识
     */
    @JvmStatic
    fun cancel(id: String) {
        synchronized(lock) {
            submittedTasks.remove(id)?.handle?.cancel()
        }
        engine.cancel(id)
    }

    /**
     * 返回所有已注册任务的 id 集合。
     */
    @JvmStatic
    fun taskIds(): Set<String> = synchronized(lock) {
        engine.taskIds() + submittedTasks.filterValues { !it.handle.isCancelled() }.keys
    }

    @JvmStatic
    fun registerSubmittedTasks() {
        val tasks = synchronized(lock) {
            if (submittedTasksRegistered) return
            submittedTasksRegistered = true
            submittedTasks.values.toList().also { submittedTasks.clear() }
        }

        start()

        val registeredIds = mutableListOf<String>()
        val errors = mutableListOf<String>()
        tasks.filterNot { it.handle.isCancelled() }.forEach { submittedTask ->
            try {
                submittedTask.handle.bind(submittedTask.register(engine))
                registeredIds += submittedTask.id
            } catch (e: Exception) {
                submittedTask.handle.cancel()
                errors += "${submittedTask.id}: ${e.message ?: e::class.simpleName ?: "unknown error"}"
            }
        }

        val ids = registeredIds.sorted()
        if (errors.isEmpty()) {
            Log.info("定时管理器启动完成，已统一注册 ${ids.size} 个定时任务：${ids.joinToString(", ")}")
        } else {
            Log.warning(
                "定时管理器启动异常，成功注册 ${ids.size} 个定时任务，失败 ${errors.size} 个：${errors.joinToString("; ")}"
            )
        }
    }

    // ───────────── 后期迁移入口 ─────────────

    /**
     * 切换底层调度引擎。
     *
     * 用于后期从 [ScheduledExecutorEngine] 迁移到 CoroutineEngine：
     * ```
     * HuYanScheduler.switchEngine(CoroutineEngine())
     * ```
     *
     * 调用后旧引擎会被 shutdown，新引擎立即生效。
     */
    internal fun switchEngine(newEngine: SchedulerEngine) {
        engine.shutdown()
        engine = newEngine
    }

    private fun submitOrRegister(task: SubmittedTask): ScheduledTask {
        return synchronized(lock) {
            if (!acceptingTasks) {
                task.handle.cancel()
                return@synchronized task.handle
            }
            if (submittedTasksRegistered) {
                if (!engine.isRunning()) {
                    task.handle.cancel()
                    task.handle
                } else {
                    runCatching { task.register(engine) }
                        .onFailure { task.handle.cancel() }
                        .getOrElse { task.handle }
                }
            } else {
                submittedTasks.remove(task.id)?.handle?.cancel()
                engine.cancel(task.id)
                submittedTasks[task.id] = task
                task.handle
            }
        }
    }

    private sealed class SubmittedTask(
        open val id: String,
        val handle: DeferredScheduledTask,
    ) {
        abstract fun register(engine: SchedulerEngine): ScheduledTask

        class Cron(
            override val id: String,
            private val cron: String,
            private val task: Runnable,
        ) : SubmittedTask(id, DeferredScheduledTask(id, cron)) {
            override fun register(engine: SchedulerEngine): ScheduledTask = engine.scheduleCron(id, cron, task)
        }

        class FixedRate(
            override val id: String,
            private val initialDelay: Long,
            private val period: Long,
            private val unit: TimeUnit,
            private val task: Runnable,
        ) : SubmittedTask(id, DeferredScheduledTask(id, null)) {
            override fun register(engine: SchedulerEngine): ScheduledTask =
                engine.scheduleFixedRate(id, initialDelay, period, unit, task)
        }

        class FixedDelay(
            override val id: String,
            private val initialDelay: Long,
            private val delay: Long,
            private val unit: TimeUnit,
            private val task: Runnable,
        ) : SubmittedTask(id, DeferredScheduledTask(id, null)) {
            override fun register(engine: SchedulerEngine): ScheduledTask =
                engine.scheduleFixedDelay(id, initialDelay, delay, unit, task)
        }

        class Once(
            override val id: String,
            private val delay: Long,
            private val unit: TimeUnit,
            private val task: Runnable,
        ) : SubmittedTask(id, DeferredScheduledTask(id, null)) {
            override fun register(engine: SchedulerEngine): ScheduledTask = engine.scheduleOnce(id, delay, unit, task)
        }
    }

    private class DeferredScheduledTask(
        override val id: String,
        override val cron: String?,
    ) : ScheduledTask {

        @Volatile
        private var delegate: ScheduledTask? = null

        @Volatile
        private var cancelled = false

        fun bind(task: ScheduledTask) {
            if (cancelled) {
                task.cancel()
            } else {
                delegate = task
            }
        }

        override fun cancel() {
            cancelled = true
            delegate?.cancel()
        }

        override fun isCancelled(): Boolean = cancelled || delegate?.isCancelled() == true

        override fun isDone(): Boolean = delegate?.isDone() ?: cancelled
    }
}
