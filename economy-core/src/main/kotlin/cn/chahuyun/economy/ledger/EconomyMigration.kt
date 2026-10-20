package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.ledger.*
import java.math.BigDecimal

/**
 * 两套账本之间的一次性资金搬迁。
 *
 * 设计约束：
 * - **单向**：`migrate` 是 mirai-economy → economy-base，`exportBase` 是反向；
 * - **幂等**：目标位置余额非 0 时一律跳过并计入"冲突"，重复执行不会翻倍；
 * - **不删源数据**：源账本保持原样，确认无误后再由人工清理；
 * - **可预演**：`dryRun = true` 只统计不写库；
 * - 金额为 0 的条目跳过；负数（债务）照常搬迁。
 */
object EconomyMigration {

    /** 单次搬迁的统计结果。 */
    data class MigrationReport(
        val dryRun: Boolean,
        val scanned: Int,
        val written: Int,
        val skippedExisting: Int,
        val failed: Int,
        val sourceTotal: BigDecimal,
        val writtenTotal: BigDecimal,
        val lines: List<String>,
    ) {
        fun format(direction: String): String = buildString {
            appendLine("经济账本搬迁（$direction）${if (dryRun) "【试运行，未写库】" else ""}")
            appendLine("扫描条目=$scanned，写入=$written，目标已存在跳过=$skippedExisting，失败=$failed")
            appendLine("源合计=${sourceTotal.setScale(2, java.math.RoundingMode.HALF_UP)}，写入合计=${writtenTotal.setScale(2, java.math.RoundingMode.HALF_UP)}")
            lines.forEach { appendLine(it) }
        }.trimEnd()
    }

    /** 两套账本当前的总额与条目数。 */
    fun status(): String = buildString {
        val base = BaseLedger
        val current = EconomyLedgerProvider.currentOrNull
        appendLine("当前经济后端: ${current?.backendId ?: "未初始化"}")
        appendLine("配置项 economyBackend=${cn.chahuyun.economy.config.EconomyConfig.economyBackend}")
        appendLine()
        appendLine("[economy-base]")
        for (book in LedgerBook.values()) {
            val entries = base.entries(book)
            appendLine("  ${book.name}: 账户=${entries.size}，合计=${sum(entries).setScale(2, java.math.RoundingMode.HALF_UP)}")
        }
        appendLine()
        if (!MiraiEconomyProbe.available()) {
            appendLine("[mirai-economy] 未安装或不可用，跳过")
            return@buildString
        }
        val remote = MiraiEconomyLedger()
        appendLine("[mirai-economy]")
        for (book in LedgerBook.values()) {
            val entries = runCatching { remote.entries(book) }.getOrElse { emptyList() }
            appendLine("  ${book.name}: 账户=${entries.size}，合计=${sum(entries).setScale(2, java.math.RoundingMode.HALF_UP)}")
        }
    }.trimEnd()

    /** mirai-economy → economy-base。 */
    fun migrate(dryRun: Boolean): MigrationReport {
        require(MiraiEconomyProbe.available()) { "未探测到上游插件 mirai-economy-core，无法执行 migrate" }
        BaseLedger.start()
        return transferAll(source = MiraiEconomyLedger(), target = BaseLedger, dryRun = dryRun, skipZeroTarget = true)
    }

    /** economy-base → mirai-economy（回滚用）。 */
    fun exportBase(dryRun: Boolean): MigrationReport {
        require(MiraiEconomyProbe.available()) { "未探测到上游插件 mirai-economy-core，无法执行 export-base" }
        BaseLedger.start()
        return transferAll(source = BaseLedger, target = MiraiEconomyLedger(), dryRun = dryRun, skipZeroTarget = false)
    }

    /**
     * 搬运主流程。
     *
     * @param skipZeroTarget true 时目标余额必须为 0 才写入（migrate 的幂等规则）；
     *                       export-base 沿用上游"累加"语义，不做该限制
     */
    private fun transferAll(
        source: EconomyLedger,
        target: EconomyLedger,
        dryRun: Boolean,
        skipZeroTarget: Boolean,
    ): MigrationReport {
        var scanned = 0
        var written = 0
        var skipped = 0
        var failed = 0
        var sourceTotal = BigDecimal.ZERO
        var writtenTotal = BigDecimal.ZERO
        val lines = mutableListOf<String>()

        for (book in LedgerBook.values()) {
            val entries = runCatching { source.entries(book) }.getOrElse { emptyList() }
                .filter { it.amount.signum() != 0 }
            if (entries.isEmpty()) continue

            var bookWritten = 0
            var bookSkipped = 0
            var bookFailed = 0
            var bookTotal = BigDecimal.ZERO

            for (entry in entries) {
                scanned++
                sourceTotal += entry.amount
                val owner = owner(book, entry.accountId)

                if (skipZeroTarget && target.balance(owner).signum() != 0) {
                    skipped++
                    bookSkipped++
                    continue
                }

                if (dryRun) {
                    written++
                    bookWritten++
                    writtenTotal += entry.amount
                    bookTotal += entry.amount
                    continue
                }

                val op = target.adjust(owner, entry.amount)
                if (op.success) {
                    written++
                    bookWritten++
                    writtenTotal += entry.amount
                    bookTotal += entry.amount
                } else {
                    failed++
                    bookFailed++
                    if (bookFailed <= 3) {
                        lines += "  [失败] ${book.name}/${entry.accountId} 金额=${entry.amount} 原因=${op.message}"
                    }
                }
            }

            lines += "  ${book.name}: 扫描=${entries.size}，写入=$bookWritten，跳过=$bookSkipped，失败=$bookFailed，" +
                "金额=${bookTotal.setScale(2, java.math.RoundingMode.HALF_UP)}"
        }

        return MigrationReport(
            dryRun = dryRun,
            scanned = scanned,
            written = written,
            skippedExisting = skipped,
            failed = failed,
            sourceTotal = sourceTotal,
            writtenTotal = writtenTotal,
            lines = lines,
        )
    }

    private fun owner(book: LedgerBook, accountId: String): LedgerOwner = when (book) {
        LedgerBook.WALLET -> LedgerOwner.Wallet(accountId)
        LedgerBook.USER_BANK -> LedgerOwner.UserBank(accountId)
        LedgerBook.PLUGIN -> LedgerOwner.PluginAccount(accountId)
        LedgerBook.GLOBAL -> LedgerOwner.GlobalAccount(accountId)
    }

    private fun sum(entries: List<LedgerEntry>): BigDecimal =
        entries.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.amount }
}
