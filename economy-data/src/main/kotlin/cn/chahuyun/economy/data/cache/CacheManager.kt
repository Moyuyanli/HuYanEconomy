package cn.chahuyun.economy.data.cache

import cn.chahuyun.economy.utils.Log
import kotlinx.serialization.KSerializer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * 缓存管理器
 *
 * 负责Redis与数据库的同步，提供统一的缓存读写接口。
 * 当前版本（Phase 1）：Redis功能仅提供框架，实际读写走DB。
 * 后续接入Redis后，只需修改此类实现即可，业务层无感知。
 */
@Suppress("UNUSED_PARAMETER")
class CacheManager(
    private val config: CacheConfig
) {
    private val delayExecutor = ScheduledThreadPoolExecutor(2).apply {
        removeOnCancelPolicy = true
    }

    private val retryQueue = ConcurrentLinkedQueue<RetryItem>()

    // ============ 读取操作 ============

    /**
     * 从缓存读取，未命中则从DB加载并回填
     */
    suspend fun <T : Any> getOrLoad(
        key: String,
        clazz: KClass<T>,
        serializer: KSerializer<T>,
        loader: suspend () -> T?,
        ttlMs: Long = config.defaultTtlMs
    ): T? {
        if (!config.redisEnabled) {
            return loader()
        }

        // TODO: Phase 2 - 实现Redis读取逻辑
        return loader()
    }

    /**
     * 批量读取
     */
    suspend fun <T : Any> multiGetOrLoad(
        keys: List<String>,
        clazz: KClass<T>,
        serializer: KSerializer<T>,
        batchLoader: suspend (List<String>) -> Map<String, T>,
        ttlMs: Long = config.defaultTtlMs
    ): Map<String, T> {
        if (!config.redisEnabled) {
            return batchLoader(keys)
        }

        // TODO: Phase 2 - 实现批量Redis读取逻辑
        return batchLoader(keys)
    }

    // ============ 写入操作 ============

    /**
     * 写入数据（延迟双删策略）
     */
    suspend fun <T : Any> writeWithSync(
        key: String,
        clazz: KClass<T>,
        serializer: KSerializer<T>,
        writer: suspend () -> T
    ): T {
        if (!config.redisEnabled) {
            return writer()
        }

        val fullKey = "${config.keyPrefix}$key"
        val data = writer()
        deleteFromRedis(fullKey)
        scheduleDelete(fullKey)
        return data
    }

    /**
     * 删除缓存
     */
    suspend fun delete(key: String) {
        if (!config.redisEnabled) return
        val fullKey = "${config.keyPrefix}$key"
        deleteFromRedis(fullKey)
    }

    /**
     * 批量删除（按模式匹配）
     */
    suspend fun deleteByPattern(pattern: String) {
        if (!config.redisEnabled) return
        val fullPattern = "${config.keyPrefix}$pattern"
        Log.debug("批量删除缓存: $fullPattern")
    }

    // ============ 缓存预热 ============

    /**
     * 缓存预热（服务启动时调用）
     */
    suspend fun warmUp(
        keys: List<String>,
        clazz: KClass<*>,
        batchLoader: suspend (List<String>) -> Map<String, Any>
    ) {
        if (!config.redisEnabled) {
            Log.info("Redis未启用，跳过缓存预热")
            return
        }

        Log.info("开始缓存预热，加载 ${keys.size} 条数据...")
        try {
            val data = batchLoader(keys)
            var loaded = 0
            data.forEach { (key, _) ->
                try {
                    loaded++
                } catch (e: Exception) {
                    Log.warning("缓存预热失败: $key")
                }
            }
            Log.info("缓存预热完成，成功加载 $loaded/${keys.size} 条")
        } catch (e: Exception) {
            Log.error("缓存预热过程发生异常", e)
        }
    }

    // ============ 生命周期 ============

    /**
     * 关闭缓存管理器
     */
    fun shutdown() {
        delayExecutor.shutdown()
        retryQueue.clear()
        Log.info("缓存管理器已关闭")
    }

    // ============ 内部方法 ============

    private fun deleteFromRedis(fullKey: String) {
        Log.debug("删除缓存: $fullKey")
    }

    private fun scheduleDelete(fullKey: String) {
        delayExecutor.schedule({
            try {
                Log.debug("延迟删除缓存: $fullKey")
            } catch (e: Exception) {
                Log.warning("延迟删除缓存失败: $fullKey")
            }
        }, config.delayDeleteMs, TimeUnit.MILLISECONDS)
    }

    private data class RetryItem(
        val key: String,
        val data: String,
        val timestamp: Long
    )
}
