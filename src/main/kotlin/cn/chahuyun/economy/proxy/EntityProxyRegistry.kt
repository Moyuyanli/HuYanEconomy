package cn.chahuyun.economy.data.proxy

import cn.chahuyun.economy.data.proxy.module.*
import cn.chahuyun.economy.utils.Log

/**
 * 瀹炰綋浠ｇ悊鍣ㄦ敞鍐岃〃銆? *
 * Phase 1 浠呮彁渚涚粺涓€娉ㄥ唽涓庢煡璇㈠叆鍙ｏ紝涓氬姟灞傝縼绉诲埌 DTO/proxy 鍦ㄥ悗缁樁娈甸€愭杩涜銆? */
object EntityProxyRegistry {

    private val proxies = linkedMapOf<String, EntityProxy<*>>()

    /**
     * 鍒濆鍖栨墍鏈夊凡缁忓叿澶?V1 閫傞厤鑳藉姏鐨勪唬鐞嗗櫒銆?     */
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

        Log.info("瀹炰綋浠ｇ悊鍣ㄦ敞鍐屽畬鎴? ${proxies.keys.joinToString()}")
    }

    fun register(proxy: EntityProxy<*>) {
        proxies[proxy.getModuleName()] = proxy
    }

    @Suppress("UNCHECKED_CAST")
    fun <D> get(module: String): EntityProxy<D>? {
        return proxies[module] as? EntityProxy<D>
    }

    fun require(module: String): EntityProxy<*> {
        return proxies[module] ?: error("瀹炰綋浠ｇ悊鍣ㄦ湭娉ㄥ唽: $module")
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
            errors = listOf("瀹炰綋浠ｇ悊妯″潡鏈敞鍐? $module")
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
        Log.info("瀹炰綋鏁版嵁婧愮増鏈凡鍒囨崲锛氬叏閮?${proxies.size} 涓ā鍧?-> $version")
    }

    private fun migrateProxy(
        module: String,
        proxy: EntityProxy<*>,
        targetVersion: DataVersion,
        switchAfterSuccess: Boolean,
    ): MigrationResult {
        Log.info("寮€濮嬭縼绉诲疄浣撴ā鍧梉$module]鍒?$targetVersion")
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
