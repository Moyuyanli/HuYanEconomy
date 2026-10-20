package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.ledger.*
import cn.chahuyun.economy.runtime.EconomyRuntime
import java.math.BigDecimal

/**
 * 上游 mirai-economy 后端。
 *
 * 位置到上游账户的映射：
 *
 * | [LedgerOwner] | 上游上下文 | 上游账户 |
 * | --- | --- | --- |
 * | `Wallet` | `custom(plugin)` | `accountId`（`u<qq>`） |
 * | `UserBank` | `global()` | `accountId`（`u<qq>`） |
 * | `PluginAccount` | `custom(plugin)` | `accountId` |
 * | `GlobalAccount` | `global()` | `accountId` |
 *
 * 上游账户主键就是 uuid（`EconomyAccountRecord.uuid = user.permitteeId.asString()`），
 * `description` 只是可变展示字段，因此这里可以直接用本插件的 `u<qq>` 作为账户标识。
 *
 * **类加载隔离约束**：本类的字段、父类、构造参数中不得出现 `xyz.cssxsh.mirai.economy.*`
 * 类型，上游类型只允许出现在方法体内部。否则上游插件未安装时，连
 * `MiraiEconomyProbe.available()` 通过后的实例化也会因为类解析失败而抛
 * `NoClassDefFoundError`（`Error`，不是 `Exception`）。
 */
internal class MiraiEconomyLedger : EconomyLedger {

    override val backendId: String = "mirai-economy"

    override fun start() {
        // 注册金币货币 + 刷新银行总额缓存 + 注册 3 小时刷新任务。
        LegacyEconomyApi.init()
        check(healthCheck()) { "mirai-economy 服务未就绪：金币货币未注册或经济上下文不可用" }
    }

    override fun stop() {
        // 上游服务由 mirai-economy 插件自身管理生命周期，这里不做关闭，避免影响其他插件。
    }

    override fun balance(owner: LedgerOwner): BigDecimal = runCatching {
        // 上游类型只出现在局部变量里：方法签名中一旦出现上游类型，未装上游的环境
        // 在个别类加载器实现下解析签名就会失败。
        val service = LegacyEconomyApi.economyService
        val context = if (isGlobal(owner)) service.global() else service.custom(EconomyRuntime.plugin)
        context.use { ctx ->
            with(ctx) {
                val account = service.account(accountId(owner), displayName(owner))
                BigDecimal.valueOf(account[LegacyEconomyApi.CURRENCY_GOLD])
            }
        }
    }.getOrElse { BigDecimal.ZERO }

    override fun add(owner: LedgerOwner, amount: BigDecimal): LedgerOp = mutate(owner, amount, plus = true)

    override fun subtract(owner: LedgerOwner, amount: BigDecimal): LedgerOp = mutate(owner, amount, plus = false)

    /**
     * 转账。
     *
     * 跨上下文（钱包 ↔ 主银行）本来就分属 custom / global 两个上游上下文，
     * 上游没有跨上下文事务能力，因此沿用"先扣后加、失败补偿"的旧语义；
     * 需要严格原子的场景请使用 economy-base 后端。
     */
    override fun transfer(from: LedgerOwner, to: LedgerOwner, amount: BigDecimal): LedgerOp {
        if (amount.signum() <= 0) return LedgerOp.fail("INVALID_AMOUNT")
        val available = balance(from)
        if (available < amount) return LedgerOp.fail("INSUFFICIENT_BALANCE", available)

        val taken = subtract(from, amount)
        if (!taken.success) return taken

        val added = add(to, amount)
        if (!added.success) {
            add(from, amount)
            return LedgerOp.fail("TRANSFER_ROLLBACK", available)
        }
        return LedgerOp.ok(balance(from))
    }

    override fun total(book: LedgerBook): BigDecimal =
        entries(book).fold(BigDecimal.ZERO) { acc, entry -> acc + entry.amount }

    /**
     * 枚举上游某个上下文的全量余额。
     *
     * 上游 `EconomyCurrency.balance()` 会按 `(uuid, context, currency)` 扫描全部余额记录，
     * 用户账户与非用户自定义账户混在一起，这里按 uuid 形态（`u<数字>`）区分。
     */
    override fun entries(book: LedgerBook): List<LedgerEntry> = runCatching {
        val custom = book == LedgerBook.WALLET || book == LedgerBook.PLUGIN
        val onlyUser = book == LedgerBook.WALLET || book == LedgerBook.USER_BANK
        balanceMap(custom)
            .asSequence()
            .filter { isUserAccount(it.key) == onlyUser }
            .map { LedgerEntry(it.key, null, BigDecimal.valueOf(it.value)) }
            .sortedBy { it.accountId }
            .toList()
    }.getOrElse { emptyList() }

    private fun mutate(owner: LedgerOwner, amount: BigDecimal, plus: Boolean): LedgerOp {
        if (amount.signum() <= 0) return LedgerOp.fail("INVALID_AMOUNT")
        return runCatching {
            val service = LegacyEconomyApi.economyService
            val context = if (isGlobal(owner)) service.global() else service.custom(EconomyRuntime.plugin)
            context.use { ctx ->
                with(ctx) {
                    val account = service.account(accountId(owner), displayName(owner))
                    if (plus) {
                        account.plusAssign(LegacyEconomyApi.CURRENCY_GOLD, amount.toDouble())
                    } else {
                        account.minusAssign(LegacyEconomyApi.CURRENCY_GOLD, amount.toDouble())
                    }
                    LedgerOp.ok(BigDecimal.valueOf(account[LegacyEconomyApi.CURRENCY_GOLD]))
                }
            }
        }.getOrElse { LedgerOp.fail(it.message) }
    }

    private fun balanceMap(custom: Boolean): Map<String, Double> {
        val service = LegacyEconomyApi.economyService
        val context = if (custom) service.custom(EconomyRuntime.plugin) else service.global()
        return context.use { ctx ->
            with(ctx) {
                LegacyEconomyApi.CURRENCY_GOLD.balance().entries
                    .associate { (account, balance) -> account.uuid to balance }
            }
        }
    }

    private fun healthCheck(): Boolean = runCatching {
        LegacyEconomyApi.economyService.basket.containsKey(LegacyEconomyApi.CURRENCY_GOLD.id)
    }.getOrElse { false }

    private fun isGlobal(owner: LedgerOwner): Boolean =
        owner is LedgerOwner.UserBank || owner is LedgerOwner.GlobalAccount

    private fun accountId(owner: LedgerOwner): String {
        val id = when (owner) {
            is LedgerOwner.Wallet -> owner.accountId
            is LedgerOwner.UserBank -> owner.accountId
            is LedgerOwner.PluginAccount -> owner.accountId
            is LedgerOwner.GlobalAccount -> owner.accountId
        }.trim()
        require(id.isNotEmpty()) { "经济账户标识无效" }
        return id
    }

    private fun displayName(owner: LedgerOwner): String? = when (owner) {
        is LedgerOwner.Wallet -> owner.displayName
        is LedgerOwner.UserBank -> owner.displayName
        else -> null
    }

    private companion object {
        private val USER_ACCOUNT = Regex("^u\\d+$")

        fun isUserAccount(accountId: String): Boolean = USER_ACCOUNT.matches(accountId)
    }
}
