package cn.chahuyun.economy.cache

import cn.chahuyun.economy.proxy.DataSourceStrategy
import cn.chahuyun.economy.utils.Log

/**
 * 缓存预热服务
 *
 * 服务启动时自动加载热点数据到Redis。
 * 预热失败不影响服务正常启动。
 */
class CacheWarmUpService(
    private val cacheManager: CacheManager,
    private val strategy: DataSourceStrategy
) {
    /**
     * 执行缓存预热，在插件onEnable阶段异步调用
     */
    suspend fun warmUp() {
        if (!strategy.isRedisEnabled()) {
            Log.info("Redis未启用，跳过缓存预热")
            return
        }

        Log.info("========== 开始缓存预热 ==========")
        try {
            // TODO: Phase 3 - 实现各模块的缓存预热逻辑
            Log.info("========== 缓存预热全部完成 ==========")
        } catch (e: Exception) {
            Log.error("缓存预热过程发生异常，服务降级为纯DB模式", e)
        }
    }
}
