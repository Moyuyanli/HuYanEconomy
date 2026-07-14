package cn.chahuyun.economy.service

import cn.chahuyun.economy.config.EconomyConfig
import cn.chahuyun.economy.utils.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Project-wide async worker pool.
 */
object EconomyAsyncService {
    private const val MIN_POOL_SIZE = 1

    private val lock = Any()
    private val threadCounter = AtomicInteger(0)

    @Volatile
    private var executor: ExecutorService? = null

    @Volatile
    private var dispatcher: ExecutorCoroutineDispatcher? = null

    @Volatile
    private var acceptingTasks = false

    @JvmStatic
    fun init() {
        synchronized(lock) {
            closeCurrent()
            acceptingTasks = true
            val poolSize = EconomyConfig.nextMessageExecutorsNumber.coerceAtLeast(MIN_POOL_SIZE)
            val newExecutor = Executors.newFixedThreadPool(poolSize) { runnable ->
                Thread(runnable, "HuYan-Async-${threadCounter.incrementAndGet()}").apply {
                    isDaemon = true
                }
            }
            executor = newExecutor
            dispatcher = newExecutor.asCoroutineDispatcher()
            Log.info("公共异步线程池启动完成，线程数：$poolSize")
        }
    }

    @JvmStatic
    fun submit(name: String, task: Runnable): Future<*>? {
        if (!acceptingTasks) {
            Log.warning("公共异步线程池未启动，任务 $name 已跳过")
            return null
        }

        val currentExecutor = ensureExecutor()
        return try {
            currentExecutor.submit {
                try {
                    task.run()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                } catch (e: Exception) {
                    Log.error("公共异步任务 $name 执行异常", e)
                }
            }
        } catch (e: RejectedExecutionException) {
            Log.warning("公共异步线程池已停止，任务 $name 已跳过")
            null
        }
    }

    @JvmStatic
    fun coroutineDispatcher(): CoroutineDispatcher = synchronized(lock) {
        ensureStartedLocked()
        dispatcher ?: error("公共异步线程池未初始化")
    }

    @JvmStatic
    fun shutdown() {
        synchronized(lock) {
            acceptingTasks = false
            closeCurrent()
            Log.info("公共异步线程池已关闭")
        }
    }

    private fun ensureExecutor(): ExecutorService = synchronized(lock) {
        ensureStartedLocked()
        executor ?: error("公共异步线程池未初始化")
    }

    private fun ensureStartedLocked() {
        if (!acceptingTasks) {
            error("公共异步线程池未启动")
        }

        val currentExecutor = executor
        if (currentExecutor != null && !currentExecutor.isShutdown && !currentExecutor.isTerminated) {
            return
        }
        val poolSize = EconomyConfig.nextMessageExecutorsNumber.coerceAtLeast(MIN_POOL_SIZE)
        val newExecutor = Executors.newFixedThreadPool(poolSize) { runnable ->
            Thread(runnable, "HuYan-Async-${threadCounter.incrementAndGet()}").apply {
                isDaemon = true
            }
        }
        executor = newExecutor
        dispatcher = newExecutor.asCoroutineDispatcher()
    }

    private fun closeCurrent() {
        dispatcher?.close()
        dispatcher = null
        executor?.shutdownNow()
        executor = null
    }
}
