package cn.chahuyun.economy.proxy

import cn.chahuyun.economy.proxy.module.*
import cn.chahuyun.economy.utils.Log

/**
 * 实体代理器注册表。
 *
 * Phase 1 仅提供统一注册与查询入口，业务层迁移到 DTO/proxy 在后续阶段逐步进行。
 */
object EntityProxyRegistry {

    private val proxies = linkedMapOf<String, EntityProxy<*>>()

    /**
     * 初始化所有已经具备 V1 适配能力的代理器。
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

        Log.info("实体代理器注册完成: ${proxies.keys.joinToString()}")
    }

    fun register(proxy: EntityProxy<*>) {
        proxies[proxy.getModuleName()] = proxy
    }

    @Suppress("UNCHECKED_CAST")
    fun <D> get(module: String): EntityProxy<D>? {
        return proxies[module] as? EntityProxy<D>
    }

    fun require(module: String): EntityProxy<*> {
        return proxies[module] ?: error("实体代理器未注册: $module")
    }

    fun modules(): Set<String> = proxies.keys

    fun currentVersions(): Map<String, DataVersion> {
        return proxies.mapValues { it.value.getCurrentVersion() }
    }

    fun migrateAllTo(targetVersion: DataVersion): Map<String, MigrationResult> {
        val results = proxies.mapValues { (module, proxy) ->
            migrateProxy(module, proxy, targetVersion)
        }
        DataSourceStrategyImpl.persistToConfig()
        return results
    }

    fun migrateModuleTo(module: String, targetVersion: DataVersion): MigrationResult {
        val proxy = proxies[module] ?: return MigrationResult.failure(
            migrated = 0,
            failed = 1,
            errors = listOf("Entity proxy module is not registered: $module")
        )
        val result = migrateProxy(module, proxy, targetVersion)
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
            setModuleVersion(module, version)
        }
        DataSourceStrategyImpl.persistToConfig()
    }

    private fun migrateProxy(module: String, proxy: EntityProxy<*>, targetVersion: DataVersion): MigrationResult {
        Log.info("Start migrating entity module[$module] to $targetVersion")
        return runCatching { proxy.migrateTo(targetVersion) }
            .onSuccess { result ->
                if (result.success) {
                    setModuleVersion(module, targetVersion)
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

    private fun setModuleVersion(module: String, version: DataVersion) {
        if (version == DataVersion.V1) {
            DataSourceStrategyImpl.clearVersion(module)
        } else {
            DataSourceStrategyImpl.setVersion(module, version)
        }
    }
}
