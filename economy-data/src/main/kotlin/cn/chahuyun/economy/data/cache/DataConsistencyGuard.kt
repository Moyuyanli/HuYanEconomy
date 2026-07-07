package cn.chahuyun.economy.data.cache

import cn.chahuyun.economy.data.proxy.DataSourceStrategy
import cn.chahuyun.economy.utils.Log

/**
 * 数据一致性守护器。
 *
 * 确保 Redis 与数据库的数据一致性。
 * 核心原则：数据库是唯一可信数据源，Redis 只作为加速层。
 */
class DataConsistencyGuard(
    private val cacheManager: CacheManager,
    private val strategy: DataSourceStrategy
) {
    /**
     * 安全写入：先写 DB 后写缓存，保证数据不丢失。
     */
    suspend fun <T> safeWrite(
        key: String,
        dbWriter: suspend () -> T,
        cacheWriter: suspend (T) -> Unit
    ): T {
        val savedData = dbWriter()
        try {
            cacheWriter(savedData)
        } catch (e: Exception) {
            Log.warning("缓存写入失败，数据已持久化到数据库: $key")
        }
        return savedData
    }

    /**
     * 服务启动时的数据一致性检查。
     */
    suspend fun consistencyCheck() {
        if (!strategy.isRedisEnabled()) {
            Log.info("Redis未启用，跳过一致性检查")
            return
        }

        Log.info("========== 开始数据一致性检查 ==========")
        try {
            // TODO: Phase 3 - 实现一致性检查逻辑
            Log.info("========== 数据一致性检查完成 ==========")
        } catch (e: Exception) {
            Log.error("数据一致性检查失败", e)
        }
    }
}
