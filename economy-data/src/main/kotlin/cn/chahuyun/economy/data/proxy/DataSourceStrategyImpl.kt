package cn.chahuyun.economy.data.proxy

import cn.chahuyun.economy.utils.Log

/**
 * Data source version strategy.
 *
 * The strategy belongs to economy-data, so configuration persistence is injected
 * by economy-main instead of reading Mirai config directly.
 */
object DataSourceStrategyImpl : DataSourceStrategy {

    private val moduleVersions = mutableMapOf<String, DataVersion>()
    private var persistVersions: ((MutableMap<String, String>) -> Unit)? = null

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

    fun configurePersistence(
        initialVersions: Map<String, String>,
        persistVersions: (MutableMap<String, String>) -> Unit,
    ) {
        this.persistVersions = persistVersions
        loadVersions(initialVersions)
    }

    fun loadVersions(configuredVersions: Map<String, String>) {
        val parsedVersions = configuredVersions.mapNotNull { (module, versionName) ->
            val version = runCatching { DataVersion.valueOf(versionName.uppercase()) }.getOrNull()
            if (version == null) {
                Log.warning("忽略无效实体数据版本配置: $module=$versionName")
                null
            } else {
                module to version
            }
        }.toMap()

        moduleVersions.clear()
        moduleVersions.putAll(parsedVersions)
    }

    fun setVersion(module: String, version: DataVersion, logChange: Boolean = true) {
        val old = moduleVersions[module]
        moduleVersions[module] = version
        if (logChange) {
            Log.info("模块[$module]数据源版本: ${old ?: "V1(默认)"} -> $version")
        }
    }

    fun setVersions(versions: Map<String, DataVersion>) {
        moduleVersions.putAll(versions)
    }

    fun clearVersion(module: String, logChange: Boolean = true) {
        val old = moduleVersions.remove(module)
        if (logChange) {
            Log.info("模块[$module]数据源版本: ${old ?: "V1(默认)"} -> V1(默认)")
        }
    }

    fun retainModules(modules: Set<String>) {
        val unknownModules = moduleVersions.keys - modules
        unknownModules.forEach { module ->
            Log.warning("移除未知实体数据版本配置: $module=${moduleVersions[module]}")
            moduleVersions.remove(module)
        }
    }

    fun persistToConfig() {
        persistVersions?.invoke(exportVersions())
    }

    fun exportVersions(): MutableMap<String, String> {
        return moduleVersions
            .filterValues { it != DataVersion.V1 }
            .mapValues { it.value.name }
            .toMutableMap()
    }

    fun getAllVersions(): Map<String, DataVersion> = moduleVersions.toMap()
}
