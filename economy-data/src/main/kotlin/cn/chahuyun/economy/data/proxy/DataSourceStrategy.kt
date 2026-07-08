package cn.chahuyun.economy.data.proxy

/**
 * 数据源策略接口。
 *
 * 决定某个模块使用哪个版本的数据源。通过配置可以控制不同模块使用不同的数据源版本，
 * 用于支持平滑迁移。
 *
 * 策略只描述“读写走哪个版本”，不直接执行迁移；迁移动作由 EntityProxyRegistry 统一调度。
 */
interface DataSourceStrategy {

    /**
     * 获取指定模块的数据源版本。
     *
     * @param module 模块名称，例如 "user"、"bank"、"fish"
     * @return 数据源版本
     */
    fun getVersion(module: String): DataVersion

    /**
     * 是否启用 Redis 缓存。
     *
     * 当前实现预留该开关，缓存层接入时应优先通过策略判断，而不是在业务模块硬编码。
     */
    fun isRedisEnabled(): Boolean

    /**
     * 获取降级链，也就是首选数据源不可用时的降级顺序。
     */
    fun getFallbackChain(module: String): List<DataVersion>
}
