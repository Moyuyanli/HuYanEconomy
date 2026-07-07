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
        if (version == DataVersion.V1) {
            DataSourceStrategyImpl.clearVersion(module, logChange)
        } else {
            DataSourceStrategyImpl.setVersion(module, version, logChange)
        }
    }
}
