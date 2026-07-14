package cn.chahuyun.economy.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * 游戏模块协程上下文。
 *
 * 主要供 action/usecase 启动短生命周期的玩法任务，例如钓鱼提醒。
 */
object GameCoroutineService : CoroutineScope {

    private val lock = Any()

    @Volatile
    private var supervisorJob = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = EconomyAsyncService.coroutineDispatcher() + supervisorJob

    fun init() {
        synchronized(lock) {
            if (!supervisorJob.isActive) {
                supervisorJob = SupervisorJob()
            }
        }
    }

    fun shutdown() {
        synchronized(lock) {
            supervisorJob.cancel()
        }
    }
}
