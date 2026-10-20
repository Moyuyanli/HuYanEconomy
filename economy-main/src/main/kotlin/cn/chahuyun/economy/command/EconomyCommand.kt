package cn.chahuyun.economy.command

import cn.chahuyun.economy.EconomyBuildConstants
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.ledger.EconomyMigration
import cn.chahuyun.economy.privatebank.PrivateBankRepairService
import cn.chahuyun.economy.privatebank.PrivateBankService
import cn.chahuyun.economy.repair.RepairManager
import cn.chahuyun.economy.repair.RepairScope
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

class EconomyCommand : CompositeCommand(
    HuYanEconomy, "hye",
    description = "壶言经济 Console 命令"
) {

    @SubCommand("v")
    @Description("查询当前壶言经济版本")
    suspend fun CommandSender.version() {
        sendMessage("当前壶言经济版本 ${EconomyBuildConstants.VERSION}")
    }

    @SubCommand("repair")
    @Description("显示数据修复范围")
    suspend fun CommandSender.repair() {
        sendMessage(RepairManager.usage())
    }

    @SubCommand("repair v1")
    @Description("仅修复 V1 数据，不读写 V2")
    suspend fun CommandSender.repairV1() {
        sendMessage(RepairManager.init(RepairScope.V1))
    }

    @SubCommand("repair v2")
    @Description("仅修复 V2 数据，不读写 V1")
    suspend fun CommandSender.repairV2() {
        sendMessage(RepairManager.init(RepairScope.V2))
    }

    @SubCommand("repair V1TOV2")
    @Description("只读 V1 备份并修复 V1 到 V2 的迁移数据")
    suspend fun CommandSender.repairV1ToV2() {
        sendMessage(RepairManager.init(RepairScope.V1_TO_V2))
    }

    @SubCommand("repair privatebank ledger")
    @Description("清理没有私人银行实体和业务记录的孤儿私人银行资金账本")
    suspend fun CommandSender.repairPrivateBankLedger(code: String) {
        val (_, message) = PrivateBankService.clearOrphanPrivateBankLedger(code)
        sendMessage(message)
    }

    @SubCommand("repair privatebank audit")
    @Description("审计私人银行资金池、放贷标的和主银行债务")
    suspend fun CommandSender.auditPrivateBank(code: String) {
        val (_, message) = PrivateBankRepairService.audit(code)
        sendMessage(message)
    }

    @SubCommand("repair privatebank deposit")
    @Description("将指定用户的私人银行本金校正为明确值，仅允许下调")
    suspend fun CommandSender.repairPrivateBankDeposit(
        code: String,
        userQq: Long,
        correctPrincipal: Double,
        confirmation: String,
    ) {
        val (_, message) = PrivateBankRepairService.correctDeposit(code, userQq, correctPrincipal, confirmation)
        sendMessage(message)
    }

    @SubCommand("repair privatebank reconcile")
    @Description("修复负放贷库存并同步放贷标的剩余额度")
    suspend fun CommandSender.reconcilePrivateBank(code: String, confirmation: String) {
        val (_, message) = PrivateBankRepairService.reconcile(code, confirmation)
        sendMessage(message)
    }

    @SubCommand("entity switch")
    @Description("切换全部实体数据使用版本，不复制数据")
    suspend fun CommandSender.entitySwitch(version: String) {
        val dataVersion = parseDataVersion(version)
        if (dataVersion == null) {
            sendMessage("不支持的数据版本: $version，可用版本: V1, V2")
            return
        }

        EntityProxyRegistry.switchAll(dataVersion)
        sendMessage("已切换全部实体数据使用版本为 $dataVersion")
    }

    @SubCommand("entity migration")
    @Description("将当前使用的数据迁移到目标版本，迁移成功后自动切换")
    suspend fun CommandSender.entityMigration(version: String) {
        val dataVersion = parseDataVersion(version)
        if (dataVersion == null) {
            sendMessage("不支持的数据版本: $version，可用版本: V1, V2")
            return
        }

        val results = EntityProxyRegistry.migrateAllTo(dataVersion, switchAfterSuccess = true)
        sendMessage(formatMigrationResult(dataVersion, results))
    }

    @SubCommand("entity version")
    @Description("显示当前实体代理数据版本")
    suspend fun CommandSender.entityVersion() {
        val versions = EntityProxyRegistry.currentVersions()
        sendMessage(
            buildString {
                appendLine("当前实体数据使用版本:")
                versions.forEach { (module, version) ->
                    appendLine("$module=$version")
                }
            }.trimEnd()
        )
    }

    @SubCommand("economy status")
    @Description("显示当前经济后端与两套账本的余额概况")
    suspend fun CommandSender.economyStatus() {
        sendMessage(EconomyMigration.status())
    }

    @SubCommand("economy migrate")
    @Description("预演：把 mirai-economy 的余额搬迁到 economy-base（只统计，不写库）")
    suspend fun CommandSender.economyMigratePreview() {
        val report = runCatching { EconomyMigration.migrate(dryRun = true) }.getOrElse {
            sendMessage("预演失败: ${it.message}")
            return
        }
        sendMessage(report.format("mirai-economy → economy-base"))
    }

    @SubCommand("economy migrate confirm")
    @Description("执行搬迁：mirai-economy → economy-base（目标已有余额的账户会跳过）")
    suspend fun CommandSender.economyMigrateConfirm(confirmation: String) {
        if (confirmation != "confirm") {
            sendMessage("该操作会真实修改余额，需要 confirm 参数确认：hye economy migrate confirm confirm")
            return
        }
        val report = runCatching { EconomyMigration.migrate(dryRun = false) }.getOrElse {
            sendMessage("搬迁失败: ${it.message}")
            return
        }
        sendMessage(report.format("mirai-economy → economy-base"))
        sendMessage("搬迁完成后请把 Config.yml 的 economyBackend 设为对应值并重启插件。")
    }

    @SubCommand("economy export-base")
    @Description("预演：把 economy-base 的余额写回 mirai-economy（只统计，不写库）")
    suspend fun CommandSender.economyExportPreview() {
        val report = runCatching { EconomyMigration.exportBase(dryRun = true) }.getOrElse {
            sendMessage("预演失败: ${it.message}")
            return
        }
        sendMessage(report.format("economy-base → mirai-economy"))
    }

    @SubCommand("economy export-base confirm")
    @Description("执行回写：economy-base → mirai-economy（上游按累加写入，执行前请确认目标为空）")
    suspend fun CommandSender.economyExportConfirm(confirmation: String) {
        if (confirmation != "confirm") {
            sendMessage("该操作会真实修改上游余额，需要 confirm 参数确认：hye economy export-base confirm confirm")
            return
        }
        val report = runCatching { EconomyMigration.exportBase(dryRun = false) }.getOrElse {
            sendMessage("回写失败: ${it.message}")
            return
        }
        sendMessage(report.format("economy-base → mirai-economy"))
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
            val status = if (result.success) "成功" else "失败"
            lines += "[$status] $module 迁移=${result.migratedCount}，失败=${result.failedCount}"
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
