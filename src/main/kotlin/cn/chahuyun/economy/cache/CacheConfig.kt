package cn.chahuyun.economy.cache

/**
 * 缓存配置
 */
data class CacheConfig(
    /** 默认缓存过期时间（毫秒），默认24小时 */
    val defaultTtlMs: Long = 24 * 60 * 60 * 1000L,
    /** 最大重试次数 */
    val maxRetries: Int = 3,
    /** 重试延迟（毫秒） */
    val retryDelayMs: Long = 100,
    /** 延迟双删间隔（毫秒） */
    val delayDeleteMs: Long = 500,
    /** 是否启用Redis */
    val redisEnabled: Boolean = false,
    /** Redis连接地址 */
    val redisUrl: String = "redis://localhost:6379",
    /** Redis密码（可选） */
    val redisPassword: String = "",
    /** Redis数据库索引 */
    val redisDatabase: Int = 0,
    /** 缓存key前缀 */
    val keyPrefix: String = "hye:"
)
