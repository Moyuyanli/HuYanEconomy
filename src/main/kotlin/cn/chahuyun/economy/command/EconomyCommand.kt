package cn.chahuyun.economy.command

import cn.chahuyun.economy.EconomyBuildConstants
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.repair.RepairManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

class EconomyCommand : CompositeCommand(
    HuYanEconomy, "hye",
    description = "HuYanEconomy Command"
) {

    @SubCommand("v")
    @Description("鏌ヨ褰撳墠澹惰█缁忔祹鐗堟湰")
    suspend fun CommandSender.version() {
        sendMessage("褰撳墠澹惰█缁忔祹鐗堟湰 ${EconomyBuildConstants.VERSION}")
    }

    @SubCommand("repair")
    @Description("修复版本迭代带来的错误")
    suspend fun CommandSender.repair() {
        sendMessage(RepairManager.init())
    }

    @SubCommand("entity switch")
    @Description("鍒囨崲鍏ㄩ儴瀹炰綋鏁版嵁浣跨敤鐗堟湰锛屼笉澶嶅埗鏁版嵁")
    suspend fun CommandSender.entitySwitch(version: String) {
        val dataVersion = parseDataVersion(version)
        if (dataVersion == null) {
            sendMessage("不支持的数据版本: ${version}，可用版本: V1, V2")
            return
        }

        EntityProxyRegistry.switchAll(dataVersion)
        sendMessage("宸插垏鎹㈠叏閮ㄥ疄浣撴暟鎹娇鐢ㄧ増鏈负 $dataVersion")
    }

    @SubCommand("entity migration")
    @Description("将当前使用的数据迁移到目标版本，迁移成功后自动切换")
    suspend fun CommandSender.entityMigration(version: String) {
        val dataVersion = parseDataVersion(version)
        if (dataVersion == null) {
            sendMessage("不支持的数据版本: ${version}，可用版本: V1, V2")
            return
        }

        val results = EntityProxyRegistry.migrateAllTo(dataVersion, switchAfterSuccess = true)
        sendMessage(formatMigrationResult(dataVersion, results))
    }

    @SubCommand("entity version")
    @Description("鏄剧ず褰撳墠瀹炰綋浠ｇ悊鏁版嵁鐗堟湰")
    suspend fun CommandSender.entityVersion() {
        val versions = EntityProxyRegistry.currentVersions()
        sendMessage(
            buildString {
                appendLine("褰撳墠瀹炰綋鏁版嵁浣跨敤鐗堟湰:")
                versions.forEach { (module, version) ->
                    appendLine("$module=$version")
                }
            }.trimEnd()
        )
    }

    @SubCommand("entity verison")
    @Description("鏄剧ず褰撳墠瀹炰綋浠ｇ悊鏁版嵁鐗堟湰")
    suspend fun CommandSender.entityVerisonAlias() {
        entityVersion()
    }

    private fun parseDataVersion(version: String): DataVersion? {
        return runCatching { DataVersion.valueOf(version.uppercase()) }.getOrNull()
            ?.takeIf { it == DataVersion.V1 || it == DataVersion.V2 }
    }

    private fun formatMigrationResult(targetVersion: DataVersion, results: Map<String, MigrationResult>): String {
        val successCount = results.count { it.value.success }
        val failedCount = results.size - successCount
        val totalMigrated = results.values.sumOf { it.migratedCount }
        val totalFailedRows = results.values.sumOf { it.failedCount }
        val lines = mutableListOf<String>()

        lines += "实体数据迁移到 $targetVersion 完成：模块 ${results.size}，成功 $successCount，失败 $failedCount"
        lines += "成功迁移的模块已自动切换到 $targetVersion。"
        lines += "迁移行数=$totalMigrated，失败行数=$totalFailedRows"
        results.forEach { (module, result) ->
            val status = if (result.success) "鎴愬姛" else "澶辫触"
            lines += "[$status] $module 杩佺Щ=${result.migratedCount}锛屽け璐?${result.failedCount}"
            result.errors.take(3).forEach { error ->
                lines += "  - $error"
            }
            if (result.errors.size > 3) {
                lines += "  - ... 还有 ${result.errors.size - 3} 个错误"
            }
        }

        return lines.joinToString("\n")
    }

}
