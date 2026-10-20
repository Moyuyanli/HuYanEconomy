package cn.chahuyun.economy.base.ledger

import cn.chahuyun.economy.base.*
import cn.chahuyun.economy.base.entity.EconomyAccountEntity
import cn.chahuyun.economy.base.ledger.BaseLedger.subtract
import cn.chahuyun.hibernateplus.HibernateFactory
import jakarta.persistence.LockModeType
import org.hibernate.Session
import java.math.BigDecimal
import java.time.Instant

/**
 * 本插件自建账户的经济后端（默认后端）。
 *
 * 位置到账户的映射：
 *
 * | [LedgerOwner] | `economy_account.account` | `economy_scope.code` |
 * | --- | --- | --- |
 * | `Wallet` | `u<qq>` | `DEFAULT` |
 * | `UserBank` | `u<qq>` | `BANK` |
 * | `PluginAccount` | 传入的账户 ID | `PLUGIN` |
 * | `GlobalAccount` | 传入的账户 ID | `GLOBAL` |
 *
 * 账户 ID 与上游 mirai-economy 的 `EconomyAccount.uuid` 完全一致（都是
 * `u<qq>` 形式的 Mirai 权限 ID），迁移时可直接按同名字符串对拷。
 */
object BaseLedger : EconomyLedger {

    override val backendId: String = "base"

    private const val SCOPE_DEFAULT = EconomyAccount.DEFAULT_SCOPE
    private const val SCOPE_BANK = "BANK"
    private const val SCOPE_PLUGIN = "PLUGIN"
    private const val SCOPE_GLOBAL = "GLOBAL"

    private const val MAX_ACCOUNT_LENGTH = 64

    /** Scope code 到显示名；DEFAULT 已由 EconomyCore.init 保证存在。 */
    private val scopeTable: Map<String, String> = linkedMapOf(
        SCOPE_DEFAULT to "默认",
        SCOPE_BANK to "主银行",
        SCOPE_PLUGIN to "插件资金",
        SCOPE_GLOBAL to "全局资金",
    )

    @Volatile
    private var scopes: Map<String, EconomyScope> = emptyMap()

    override fun start() {
        scopes = scopeTable.mapValues { (code, name) ->
            EconomyAccount.scope(code) ?: runCatching { EconomyAccount.scopeCreate(name, code) }
                .getOrElse { EconomyAccount.scope(code) ?: error("经济 Scope 初始化失败: $code") }
        }
    }

    override fun stop() {
        scopes = emptyMap()
    }

    override fun balance(owner: LedgerOwner): BigDecimal =
        runCatching { account(owner).balance.value }.getOrElse { BigDecimal.ZERO }

    override fun add(owner: LedgerOwner, amount: BigDecimal): LedgerOp = runCatching {
        toOp(EconomyAction.add(account(owner), EconomyBalance.of(amount)))
    }.getOrElse { LedgerOp.fail(it.message) }

    override fun subtract(owner: LedgerOwner, amount: BigDecimal): LedgerOp = runCatching {
        toOp(EconomyAction.subtract(account(owner), EconomyBalance.of(amount)))
    }.getOrElse { LedgerOp.fail(it.message) }

    override fun transfer(from: LedgerOwner, to: LedgerOwner, amount: BigDecimal): LedgerOp = runCatching {
        toOp(EconomyAction.transfer(account(from), account(to), EconomyBalance.of(amount)))
    }.getOrElse { LedgerOp.fail(it.message) }

    /**
     * 带符号增减；负数分支**允许把余额扣成负数**。
     *
     * 该分支只服务于"债务转移"类场景（例如私人银行破产清算把债务转到行长账户，
     * 行长余额可能不足以覆盖）。普通扣款必须走 [subtract]，由 EconomyCore 保证
     * 不会出现负余额。
     */
    override fun adjust(owner: LedgerOwner, amount: BigDecimal): LedgerOp = when {
        amount.signum() > 0 -> add(owner, amount)
        amount.signum() < 0 -> forceDebit(owner, amount.negate())
        else -> LedgerOp.ok(balance(owner))
    }

    /**
     * 按分类统计总额。
     *
     * 沿用旧实现的语义：直接对账本内所有账户求和（包含负数余额），因此与
     * mirai-economy 的 `getBankTotalCached()` 口径一致。账户量级在千级，
     * 内存求和不引入额外 SQL 方言风险。
     */
    override fun total(book: LedgerBook): BigDecimal =
        entries(book).fold(BigDecimal.ZERO) { acc, entry -> acc + entry.amount }

    override fun entries(book: LedgerBook): List<LedgerEntry> {
        val code = scopeCode(book)
        return runCatching {
            read { session ->
                session.createQuery(
                    "select a from EconomyAccountEntity a join a.scope s where s.code = :code order by a.account",
                    EconomyAccountEntity::class.java,
                ).setParameter("code", code).list()
            }.map { LedgerEntry(it.account, it.description, it.balance) }
        }.getOrElse { emptyList() }
    }

    /** 取得（必要时创建）某个位置对应的账户句柄。 */
    private fun account(owner: LedgerOwner): EconomyAccount =
        EconomyAccount.take(accountId(owner), scope(owner))

    /**
     * 不受余额下限保护的扣减，用于债务转移。
     *
     * 使用悲观写锁保证并发安全；金额越界（DB 精度）时由数据库拒绝并转为失败结果。
     */
    private fun forceDebit(owner: LedgerOwner, amount: BigDecimal): LedgerOp = runCatching {
        val id = account(owner).id
        HibernateFactory.getSessionFactory().fromTransaction { session ->
            val entity = session.find(EconomyAccountEntity::class.java, id, LockModeType.PESSIMISTIC_WRITE)
                ?: return@fromTransaction LedgerOp.fail("ACCOUNT_MISSING")
            entity.balance = entity.balance.subtract(amount)
            entity.updateTime = Instant.now()
            LedgerOp.ok(entity.balance)
        }
    }.getOrElse { LedgerOp.fail(it.message) }

    private fun scope(owner: LedgerOwner): EconomyScope {
        val code = scopeCode(
            when (owner) {
                is LedgerOwner.Wallet -> LedgerBook.WALLET
                is LedgerOwner.UserBank -> LedgerBook.USER_BANK
                is LedgerOwner.PluginAccount -> LedgerBook.PLUGIN
                is LedgerOwner.GlobalAccount -> LedgerBook.GLOBAL
            }
        )
        return scopes[code] ?: EconomyAccount.scope(code) ?: error("经济 Scope 未初始化: $code")
    }

    private fun scopeCode(book: LedgerBook): String = when (book) {
        LedgerBook.WALLET -> SCOPE_DEFAULT
        LedgerBook.USER_BANK -> SCOPE_BANK
        LedgerBook.PLUGIN -> SCOPE_PLUGIN
        LedgerBook.GLOBAL -> SCOPE_GLOBAL
    }

    private fun accountId(owner: LedgerOwner): String {
        val id = when (owner) {
            is LedgerOwner.Wallet -> owner.accountId
            is LedgerOwner.UserBank -> owner.accountId
            is LedgerOwner.PluginAccount -> owner.accountId
            is LedgerOwner.GlobalAccount -> owner.accountId
        }.trim()
        require(id.isNotEmpty() && id.length <= MAX_ACCOUNT_LENGTH) { "经济账户标识无效: $id" }
        return id
    }

    private fun toOp(result: EconomyActionResult): LedgerOp =
        LedgerOp(result.success, result.balance?.value, result.status.name)

    private fun <T> read(block: (Session) -> T): T =
        HibernateFactory.getSessionFactory().fromTransaction { session ->
            session.isDefaultReadOnly = true
            block(session)
        }
}
