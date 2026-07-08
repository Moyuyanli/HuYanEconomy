package cn.chahuyun.economy.data.proxy

import cn.chahuyun.economy.data.proxy.module.*
import cn.chahuyun.economy.utils.Log

/**
 * 实体代理注册表。
 *
 * 负责集中注册 data 模块内的实体代理，并为业务层提供统一查询、
 * 迁移与数据源版本切换入口。
 */
object EntityProxyRegistry {

    /** 保持注册顺序稳定，日志和批量迁移结果会按该顺序输出。 */
    private val proxies = linkedMapOf<String, EntityProxy<*>>()

    /**
     * 初始化所有已接入 data proxy 的实体代理。
     */
    fun init() {
        proxies.clear()
        register(UserEntityProxy())
        register(UserBackpackEntityProxy())
        register(UserStatusEntityProxy())
        register(UserFactorEntityProxy())
        register(UserRaffleEntityProxy())
        register(BankEntityProxy())
        register(FishEntityProxy())
        register(FishInfoEntityProxy())
        register(FishPondEntityProxy())
        register(RedPackEntityProxy())
        register(RaffleEntityProxy())
        register(PropsEntityProxy())
        register(GlobalFactorEntityProxy())
        register(LotteryInfoEntityProxy())
        register(RobInfoEntityProxy())
        register(TitleInfoEntityProxy())
        register(PrivateBankEntityProxy())

        DataSourceStrategyImpl.retainModules(proxies.keys)
        DataSourceStrategyImpl.persistToConfig()

        Log.info("实体代理注册完成: ${proxies.keys.joinToString()}")
    }

    fun register(proxy: EntityProxy<*>) {
        // 同名模块后注册会覆盖旧代理，方便测试或未来替换实现，但生产启动应避免重复模块名。
        proxies[proxy.getModuleName()] = proxy
    }

    @Suppress("UNCHECKED_CAST")
    fun <D> get(module: String): EntityProxy<D>? {
        return proxies[module] as? EntityProxy<D>
    }

    fun require(module: String): EntityProxy<*> {
        return proxies[module] ?: error("实体代理未注册: $module")
    }

    fun modules(): Set<String> = proxies.keys

    fun currentVersions(): Map<String, DataVersion> {
        return proxies.mapValues { it.value.getCurrentVersion() }
    }

    fun migrateAllTo(targetVersion: DataVersion, switchAfterSuccess: Boolean = false): Map<String, MigrationResult> {
        // 批量迁移逐模块独立捕获异常，避免单个模块失败中断其它模块的迁移报告。
        val results = proxies.mapValues { (module, proxy) ->
            migrateProxy(module, proxy, targetVersion, switchAfterSuccess)
        }
        DataSourceStrategyImpl.persistToConfig()
        return results
    }

    fun migrateModuleTo(module: String, targetVersion: DataVersion, switchAfterSuccess: Boolean = false): MigrationResult {
        val proxy = proxies[module] ?: return MigrationResult.failure(
            migrated = 0,
            failed = 1,
            errors = listOf("实体代理模块未注册: $module")
        )
        val result = migrateProxy(module, proxy, targetVersion, switchAfterSuccess)
        DataSourceStrategyImpl.persistToConfig()
        return result
    }

    fun switchModule(module: String, version: DataVersion): Boolean {
        if (!proxies.containsKey(module)) return false
        setModuleVersion(module, version)
        DataSourceStrategyImpl.persistToConfig()
        return true
    }

    fun switchAll(version: DataVersion) {
        proxies.keys.forEach { module ->
            setModuleVersion(module, version, logChange = false)
        }
        DataSourceStrategyImpl.persistToConfig()
        Log.info("实体数据源版本已切换: 全部 ${proxies.size} 个模块 -> $version")
    }

    private fun migrateProxy(
        module: String,
        proxy: EntityProxy<*>,
        targetVersion: DataVersion,
        switchAfterSuccess: Boolean,
    ): MigrationResult {
        Log.info("开始迁移实体模块[$module]到$targetVersion")
        return runCatching { proxy.migrateTo(targetVersion) }
            .onSuccess { result ->
                if (result.success && switchAfterSuccess) {
                    setModuleVersion(module, targetVersion, logChange = false)
                }
            }
            .getOrElse { e ->
                MigrationResult.failure(
                    migrated = 0,
                    failed = 1,
                    errors = listOf(e.message ?: e::class.simpleName ?: "unknown error")
                )
            }
    }

    private fun setModuleVersion(module: String, version: DataVersion, logChange: Boolean = true) {
        // V1 是默认版本，不写入配置；只有非默认版本才持久化，保持配置文件简洁。
        if (version == DataVersion.V1) {
            DataSourceStrategyImpl.clearVersion(module, logChange)
        } else {
            DataSourceStrategyImpl.setVersion(module, version, logChange)
        }
    }
}
