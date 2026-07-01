package cn.chahuyun.economy.proxy

import cn.chahuyun.economy.config.EconomyPluginConfig
import cn.chahuyun.economy.proxy.DataSourceStrategyImpl.setVersion
import cn.chahuyun.economy.utils.Log

/**
 * 数据源策略实现（单例）
 *
 * 通过配置文件控制各模块的数据源版本。
 * 默认所有模块使用V1，可通过 [setVersion] 动态切换。
 */
object DataSourceStrategyImpl : DataSourceStrategy {

    private val moduleVersions = mutableMapOf<String, DataVersion>()

    override fun getVersion(module: String): DataVersion {
        return moduleVersions[module] ?: DataVersion.V1
    }

    override fun isRedisEnabled(): Boolean {
        return false
    }

    override fun getFallbackChain(module: String): List<DataVersion> {
        val primary = getVersion(module)
        return listOf(primary, DataVersion.V1).distinct()
    }

    /**
     * 设置模块的数据源版本
     */
    fun setVersion(module: String, version: DataVersion) {
        val old = moduleVersions[module]
        moduleVersions[module] = version
        Log.info("模块[$module]数据源版本: ${old ?: "V1(默认)"} -> $version")
    }

    /**
     * 批量设置模块版本
     */
    fun setVersions(versions: Map<String, DataVersion>) {
        versions.forEach { (module, version) ->
            setVersion(module, version)
        }
    }

    fun clearVersion(module: String) {
        val old = moduleVersions.remove(module)
        Log.info("妯″潡[$module]鏁版嵁婧愮増鏈? ${old ?: "V1(榛樿)"} -> V1(榛樿)")
    }

    fun retainModules(modules: Set<String>) {
        val unknownModules = moduleVersions.keys - modules
        unknownModules.forEach { module ->
            Log.warning("Remove unknown entity data version config: $module=${moduleVersions[module]}")
            moduleVersions.remove(module)
        }
    }

    fun loadFromConfig() {
        val configuredVersions = EconomyPluginConfig.entityDataVersions.mapNotNull { (module, versionName) ->
            val version = runCatching { DataVersion.valueOf(versionName.uppercase()) }.getOrNull()
            if (version == null) {
                Log.warning("Ignore invalid entity data version config: $module=$versionName")
                null
            } else {
                module to version
            }
        }.toMap()

        moduleVersions.clear()
        setVersions(configuredVersions)
    }

    fun persistToConfig() {
        EconomyPluginConfig.entityDataVersions = moduleVersions
            .filterValues { it != DataVersion.V1 }
            .mapValues { it.value.name }
            .toMutableMap()
    }

    /**
     * 获取所有模块的版本配置
     */
    fun getAllVersions(): Map<String, DataVersion> = moduleVersions.toMap()
}
