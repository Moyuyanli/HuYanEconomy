package cn.chahuyun.economy.scheduler

import java.util.concurrent.TimeUnit

/**
 * 调度引擎接口。
 *
 * 现阶段实现: [ScheduledExecutorEngine]
 * 后期实现: CoroutineEngine (基于 kotlinx.coroutines delay)
 *
 * @author Moyuyanli
 * @since 2.0.0
 */
internal interface SchedulerEngine {

    /**
     * 启动引擎。
     */
    fun start()

    /**
     * 瞬间停止引擎，不等待正在执行的任务完成。
     */
    fun shutdown()

    /**
     * 引擎是否正在运行。
     */
    fun isRunning(): Boolean

    /**
     * 注册 cron 定时任务。
     *
     * @param id   任务唯一标识，重复 id 会覆盖旧任务
     * @param cron cron 表达式（支持 Hutool 语法，含 `?` 通配符）
     * @param task 要执行的任务
     * @return 任务句柄
     */
    fun scheduleCron(id: String, cron: String, task: Runnable): ScheduledTask

    /**
     * 注册固定频率任务（fixed-rate）。
     *
     * @param id            任务唯一标识
     * @param initialDelay  首次执行延迟
     * @param period        执行周期
     * @param unit          时间单位
     * @param task          要执行的任务
     * @return 任务句柄
     */
    fun scheduleFixedRate(id: String, initialDelay: Long, period: Long, unit: TimeUnit, task: Runnable): ScheduledTask

    /**
     * 注册固定延迟任务（fixed-delay，上一次结束到下一次开始之间的延迟）。
     *
     * @param id            任务唯一标识
     * @param initialDelay  首次执行延迟
     * @param delay         两次执行之间的延迟
     * @param unit          时间单位
     * @param task          要执行的任务
     * @return 任务句柄
     */
    fun scheduleFixedDelay(id: String, initialDelay: Long, delay: Long, unit: TimeUnit, task: Runnable): ScheduledTask

    /**
     * 注册一次性延迟任务。
     *
     * @param id    任务唯一标识
     * @param delay 延迟时间
     * @param unit  时间单位
     * @param task  要执行的任务
     * @return 任务句柄
     */
    fun scheduleOnce(id: String, delay: Long, unit: TimeUnit, task: Runnable): ScheduledTask

    /**
     * 取消指定任务。
     *
     * @param id 任务唯一标识
     */
    fun cancel(id: String)

    /**
     * 返回所有已注册任务的 id 集合。
     */
    fun taskIds(): Set<String>
}
