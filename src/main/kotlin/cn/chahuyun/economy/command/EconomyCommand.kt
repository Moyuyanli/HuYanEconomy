package cn.chahuyun.economy.command

import cn.chahuyun.economy.EconomyBuildConstants
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.proxy.DataVersion
import cn.chahuyun.economy.proxy.EntityProxyRegistry
import cn.chahuyun.economy.proxy.MigrationResult
import cn.chahuyun.economy.repair.RepairManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

class EconomyCommand : CompositeCommand(
    HuYanEconomy, "hye",
    description = "HuYanEconomy Command"
) {

    @SubCommand("v")
    @Description("查询当前壶言经济版本")
    suspend fun CommandSender.version() {
        sendMessage("当前壶言经济版本 ${EconomyBuildConstants.VERSION}")
    }

    @SubCommand("repair")
    @Description("修复版本迭代带来的错误")
    suspend fun CommandSender.repair() {
        sendMessage(RepairManager.init())
    }

    @SubCommand("migrate v2")
    @Description("Migrate entity data from V1 tables to V2 tables and switch successful modules to V2")
    suspend fun CommandSender.migrateV2(module: String = "all") {
        if (module == "all") {
            val results = EntityProxyRegistry.migrateAllTo(DataVersion.V2)
            sendMessage(formatMigrationResult("V2", results))
            return
        }

        val result = EntityProxyRegistry.migrateModuleTo(module, DataVersion.V2)
        sendMessage(formatMigrationResult("V2", mapOf(module to result)))
    }

    @SubCommand("entity switch")
    @Description("Switch entity proxy module data version without copying data")
    suspend fun CommandSender.entitySwitch(module: String, version: String) {
        val dataVersion = parseDataVersion(version)
        if (dataVersion == null) {
            sendMessage("Unsupported entity data version: $version")
            return
        }

        if (module == "all") {
            EntityProxyRegistry.switchAll(dataVersion)
            sendMessage("All entity proxy modules switched to $dataVersion")
            return
        }

        val switched = EntityProxyRegistry.switchModule(module, dataVersion)
        sendMessage(if (switched) "Entity proxy module[$module] switched to $dataVersion" else "Unknown entity proxy module: $module")
    }

    @SubCommand("rollback v1")
    @Description("Switch entity proxy modules back to V1")
    suspend fun CommandSender.rollbackV1(module: String = "all") {
        if (module == "all") {
            EntityProxyRegistry.switchAll(DataVersion.V1)
            sendMessage("All entity proxy modules switched back to V1")
            return
        }

        val switched = EntityProxyRegistry.switchModule(module, DataVersion.V1)
        sendMessage(if (switched) "Entity proxy module[$module] switched back to V1" else "Unknown entity proxy module: $module")
    }

    @SubCommand("entity version")
    @Description("Show current entity proxy data versions")
    suspend fun CommandSender.entityVersion() {
        val versions = EntityProxyRegistry.currentVersions()
        sendMessage(
            buildString {
                appendLine("Entity data versions:")
                versions.forEach { (module, version) ->
                    appendLine("$module=$version")
                }
            }.trimEnd()
        )
    }

    private fun parseDataVersion(version: String): DataVersion? {
        return runCatching { DataVersion.valueOf(version.uppercase()) }.getOrNull()
            ?.takeIf { it == DataVersion.V1 || it == DataVersion.V2 }
    }

    private fun formatMigrationResult(targetVersion: String, results: Map<String, MigrationResult>): String {
        val successCount = results.count { it.value.success }
        val failedCount = results.size - successCount
        val totalMigrated = results.values.sumOf { it.migratedCount }
        val totalFailedRows = results.values.sumOf { it.failedCount }
        val lines = mutableListOf<String>()

        lines += "Entity $targetVersion migration finished: modules=${results.size}, success=$successCount, failed=$failedCount"
        lines += "Rows migrated=$totalMigrated, failed=$totalFailedRows"
        results.forEach { (module, result) ->
            val status = if (result.success) "OK" else "FAILED"
            lines += "[$status] $module migrated=${result.migratedCount}, failed=${result.failedCount}"
            result.errors.take(3).forEach { error ->
                lines += "  - $error"
            }
            if (result.errors.size > 3) {
                lines += "  - ... ${result.errors.size - 3} more errors"
            }
        }

        return lines.joinToString("\n")
    }

}
