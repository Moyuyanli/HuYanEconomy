package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.ledger.EconomyLedger
import cn.chahuyun.economy.base.ledger.LedgerBook
import cn.chahuyun.economy.base.ledger.LedgerEntry
import cn.chahuyun.economy.base.ledger.LedgerOp
import cn.chahuyun.economy.base.ledger.LedgerOwner
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.utils.Log
import java.math.BigDecimal

/**
 * 上游 mirai-economy 后端。
 *
 * ## 位置 → 上游账户
 *
 * 上游的账户坐标是 `(uuid, context)`，其中 `context` 只有固定几个值
 * （`global-economy` 与各插件自己的 `plugin.id`），无法承载本插件可自由新增的
 * scope。因此本实现**不使用 `global()` 上下文**，全部落 `custom(plugin)`，
 * 把 scope 编码进账户 id 后缀：
 *
 * | [LedgerOwner] | 账户 id | 上下文 |
 * | --- | --- | --- |
 * | `Wallet` | `u<qq>`（无后缀） | `custom(plugin)` |
 * | `UserBank` | `u<qq>#BANK` | `custom(plugin)` |
 * | `PluginAccount` | `<id>#PLUGIN` | `custom(plugin)` |
 * | `GlobalAccount` | `<id>#GLOBAL` | `custom(plugin)` |
 *
 * `Wallet` 不加后缀是刻意的：上游历史数据里用户钱包本来就存在
 * `(custom, u<qq>)`，保持同名可以让存量钱包零搬迁直接可用。
 *
 * 分隔符选 `#`：QQ 号与既有插件账户名都不含该字符；解析时以**最后一个**
 * `#` 切分并校验后缀白名单，因此账户名本身含 `#` 也不会被误判。
 *
 * 附带收益：`PluginAccount` 加上 `#PLUGIN` 后不再匹配 `^u\d+$`，即便有人误传
 * `u999` 作为插件账户名，也不会被误认成用户钱包。
 *
 * ## 类加载隔离约束
 *
 * 本类的字段、父类、构造参数中不得出现 `xyz.cssxsh.mirai.economy.*` 类型，
 * 上游类型只允许出现在方法体内部。否则上游插件未安装时，即使探测通过，
 * 实例化也会因为类解析失败而抛 `NoClassDefFoundError`（`Error`，不是 `Exception`）。
 */
internal class MiraiEconomyLedger(
    private val layout: Layout = Layout.SCOPE_SUFFIX,
) : EconomyLedger {

    /** 上游账户命名布局。 */
    internal enum class Layout {
        /** 当前布局：全部 `custom(plugin)`，scope 编码进 id 后缀。 */
        SCOPE_SUFFIX,

        /**
         * 历史布局（≤ 2.0.1）：钱包与插件池在 `custom`，主银行与全局池在 `global`，
         * id 不带后缀。**仅供迁移读取**，不要用于运行时读写。
         */
        LEGACY_CONTEXT,
    }

    override val backendId: String = "mirai-economy"

    override fun start() {
        // 注册金币货币。上游服务由 mirai-economy 插件自身管理生命周期。
        LegacyEconomyApi.init()
        check(healthCheck()) { "mirai-economy 服务未就绪：金币货币未注册或经济上下文不可用" }
        if (layout == Layout.SCOPE_SUFFIX) {
            warnLegacyResidue()
        }
    }

    override fun stop() {
        // 不关闭上游服务，避免影响其他插件。
    }

    override fun balance(owner: LedgerOwner): BigDecimal = runCatching {
        // 上游类型只出现在局部变量里：方法签名中一旦出现上游类型，未装上游的环境
        // 在个别类加载器实现下解析签名就会失败。
        val target = resolve(owner)
        val service = LegacyEconomyApi.economyService
        val context = if (target.global) service.global() else service.custom(EconomyRuntime.plugin)
        context.use { ctx ->
            with(ctx) {
                val account = service.account(target.id, target.description)
                BigDecimal.valueOf(account[LegacyEconomyApi.CURRENCY_GOLD])
            }
        }
    }.getOrElse { BigDecimal.ZERO }

    override fun add(owner: LedgerOwner, amount: BigDecimal): LedgerOp = mutate(owner, amount, plus = true)

    override fun subtract(owner: LedgerOwner, amount: BigDecimal): LedgerOp = mutate(owner, amount, plus = false)

    /**
     * 转账。
     *
     * 上游没有暴露跨账户事务 API，因此沿用"先扣后加、失败补偿"的语义。
     * [Layout.SCOPE_SUFFIX] 下两端虽同处一个上下文，上游仍不提供事务句柄，
     * 补偿逻辑保持一致。需要严格原子的场景请使用 economy-base 后端。
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
     * 枚举分类下的全量余额。
     *
     * `SCOPE_SUFFIX` 完全按 id 后缀区分（`EconomyAccountRecord.description` 只是
     * 可变展示字段，不能作为身份依据）；`LEGACY_CONTEXT` 按上下文 + uuid 形态区分。
     */
    override fun entries(book: LedgerBook): List<LedgerEntry> = runCatching {
        when (layout) {
            Layout.SCOPE_SUFFIX -> entriesBySuffix(book)
            Layout.LEGACY_CONTEXT -> entriesByContext(book)
        }
    }.getOrElse { emptyList() }

    private fun entriesBySuffix(book: LedgerBook): List<LedgerEntry> =
        balanceMap(custom = true).asSequence()
            .mapNotNull { (rawId, balance) ->
                val decoded = MiraiAccountId.decode(rawId)
                if (MiraiAccountId.belongsTo(decoded, book)) {
                    LedgerEntry(decoded.accountId, null, BigDecimal.valueOf(balance))
                } else {
                    null
                }
            }
            .sortedBy { it.accountId }
            .toList()

    private fun entriesByContext(book: LedgerBook): List<LedgerEntry> {
        val custom = book == LedgerBook.WALLET || book == LedgerBook.PLUGIN
        val onlyUser = book == LedgerBook.WALLET || book == LedgerBook.USER_BANK
        return balanceMap(custom).asSequence()
            .filter { MiraiAccountId.isUserAccount(it.key) == onlyUser }
            .map { LedgerEntry(it.key, null, BigDecimal.valueOf(it.value)) }
            .sortedBy { it.accountId }
            .toList()
    }

    private fun mutate(owner: LedgerOwner, amount: BigDecimal, plus: Boolean): LedgerOp {
        if (amount.signum() <= 0) return LedgerOp.fail("INVALID_AMOUNT")
        return runCatching {
            val target = resolve(owner)
            val service = LegacyEconomyApi.economyService
            val context = if (target.global) service.global() else service.custom(EconomyRuntime.plugin)
            context.use { ctx ->
                with(ctx) {
                    val account = service.account(target.id, target.description)
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

    /**
     * 启动自检：新布局下若历史布局仍有余额，说明尚未执行 `hye economy migrate`，
     * 此时读写会落在空账户上，必须显式告警。
     */
    private fun warnLegacyResidue() {
        val residue = runCatching {
            val legacy = MiraiEconomyLedger(Layout.LEGACY_CONTEXT)
            LedgerBook.values().sumOf { book ->
                legacy.entries(book).count { it.amount.signum() != 0 }
            }
        }.getOrElse { 0 }
        if (residue > 0) {
            Log.warning(
                "mirai-economy 检测到 $residue 条历史布局余额（钱包在 custom、主银行在 global）。" +
                    "当前已改用「单上下文 + scope 后缀」布局，这些余额不会被自动读取，" +
                    "请先执行 hye economy migrate 再切换后端。",
            )
        }
    }

    /** 解析上游账户坐标：账户 id、展示名、是否走 global 上下文。 */
    private fun resolve(owner: LedgerOwner): Target = when (layout) {
        Layout.SCOPE_SUFFIX -> Target(MiraiAccountId.encode(owner), displayName(owner), global = false)
        Layout.LEGACY_CONTEXT -> Target(MiraiAccountId.rawId(owner), displayName(owner), global = isGlobalOwner(owner))
    }

    private fun isGlobalOwner(owner: LedgerOwner): Boolean =
        owner is LedgerOwner.UserBank || owner is LedgerOwner.GlobalAccount

    private fun displayName(owner: LedgerOwner): String? = when (owner) {
        is LedgerOwner.Wallet -> owner.displayName
        is LedgerOwner.UserBank -> owner.displayName
        else -> null
    }

    private data class Target(val id: String, val description: String?, val global: Boolean)
}
