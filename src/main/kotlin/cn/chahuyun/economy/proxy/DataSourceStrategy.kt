package cn.chahuyun.economy.proxy

/**
 * 数据源策略接口
 *
 * 决定某个模块使用哪个版本的数据源。
 * 通过配置可以控制不同模块使用不同的数据源版本，实现平滑迁移。
 */
interface DataSourceStrategy {

    /**
     * 获取指定模块的数据源版本
     *
     * @param module 模块名称，如 "user", "bank", "fish" 等
     * @return 数据源版本
     */
    fun getVersion(module: String): DataVersion

    /**
     * 是否启用Redis缓存
     */
    fun isRedisEnabled(): Boolean

    /**
     * 获取降级链（当首选数据源不可用时的降级顺序）
     */
    fun getFallbackChain(module: String): List<DataVersion>
}
