package cn.chahuyun.economy.base.ledger

import cn.chahuyun.economy.base.*
import cn.chahuyun.economy.base.entity.EconomyAccountEntity
import cn.chahuyun.economy.base.ledger.BaseLedger.MAX_ACCOUNT_LENGTH
import cn.chahuyun.economy.base.ledger.BaseLedger.account
import cn.chahuyun.economy.base.ledger.BaseLedger.adjust
import cn.chahuyun.economy.base.ledger.BaseLedger.entries
import cn.chahuyun.economy.base.ledger.BaseLedger.scope
import cn.chahuyun.economy.base.ledger.BaseLedger.scopes
import cn.chahuyun.economy.base.ledger.BaseLedger.start
import cn.chahuyun.economy.base.ledger.BaseLedger.stop
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
 *
 * ## 事务模型
 * - 普通增减 / 转账：每次操作一个 `fromTransaction`，由 `EconomyAction` 保证"先校验后写"；
 *   转账两端在同一事务内完成，失败整体回滚，**不需要补偿**。
 * - 强制扣款（[adjust] 的负数分支）：单条实体的悲观写锁（`PESSIMISTIC_WRITE`），
 *   只锁一行，避免与普通扣款并发时丢更新。
 * - 聚合查询（[entries]）：只读事务 + `isDefaultReadOnly`，避免脏读并允许驱动走只读优化。
 *
 * ## 异常策略
 * 所有方法用 `runCatching` 兜底转成 [LedgerOp.fail] 或零值，业务层永远拿不到 DB 异常。
 * 代价是"表不可用"这类故障会被表现为"余额 0 / 操作失败"，因此排查时要看日志里的
 * `backendId` 与 `message`，不能只看返回值。
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

    /**
     * [start] 时解析好的 Scope 实体缓存。
     *
     * 只在 [start] 写、之后只读，因此用 `@Volatile` 即可保证可见性，无需加锁；
     * [stop] 清空后若再被调用，[scope] 会回落到一次库查询（或抛错），不会读到脏引用。
     */
    @Volatile
    private var scopes: Map<String, EconomyScope> = emptyMap()

    /**
     * 启动自检：确保四个 Scope 存在。
     *
     * 先查后建，容忍并发创建的竞态（创建失败时回查一次）；任一 Scope 无法就绪即抛异常，
     * 由 `EconomyLedgerProvider` 决定是启动失败还是降级。重复调用可安全执行。
     */
    override fun start() {
        scopes = scopeTable.mapValues { (code, name) ->
            EconomyAccount.scope(code) ?: runCatching { EconomyAccount.scopeCreate(name, code) }
                .getOrElse { EconomyAccount.scope(code) ?: error("经济 Scope 初始化失败: $code") }
        }
    }

    /** 释放缓存；不关闭 Hibernate 会话工厂（它由 `HibernateUtil` 管理）。 */
    override fun stop() {
        scopes = emptyMap()
    }

    /**
     * 读取余额。
     *
     * [account] 在账户不存在时会建零余额账户（与上游 `service.account(...)` 的行为对齐），
     * 因此"查余额"这个只读动作也可能写库；异常一律返回 0。
     */
    override fun balance(owner: LedgerOwner): BigDecimal =
        runCatching { account(owner).balance.value }.getOrElse { BigDecimal.ZERO }

    /** 入账；金额非正或越界由 `EconomyBalance.of` / 数据库拒绝，转成失败结果。 */
    override fun add(owner: LedgerOwner, amount: BigDecimal): LedgerOp = runCatching {
        toOp(EconomyAction.add(account(owner), EconomyBalance.of(amount)))
    }.getOrElse { LedgerOp.fail(it.message) }

    /** 扣款；余额不足由 `EconomyAction.subtract` 拦截，**不会写出负余额**。 */
    override fun subtract(owner: LedgerOwner, amount: BigDecimal): LedgerOp = runCatching {
        toOp(EconomyAction.subtract(account(owner), EconomyBalance.of(amount)))
    }.getOrElse { LedgerOp.fail(it.message) }

    /**
     * 转账；两端账户在同一事务内处理，任一端失败整体回滚，因此本实现不需要补偿逻辑。
     */
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

    /**
     * 取位置对应的 `EconomyScope`：先命中 [scopes] 缓存，未命中才回落到库查询。
     *
     * 回落分支是给"缓存被 [stop] 清空后仍有调用"的场景兜底；正常生命周期下不应触发。
     */
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

    /** 账本分类 → 数据库里的 Scope code；这是 [LedgerBook] 与 `economy_scope` 之间唯一的映射点。 */
    private fun scopeCode(book: LedgerBook): String = when (book) {
        LedgerBook.WALLET -> SCOPE_DEFAULT
        LedgerBook.USER_BANK -> SCOPE_BANK
        LedgerBook.PLUGIN -> SCOPE_PLUGIN
        LedgerBook.GLOBAL -> SCOPE_GLOBAL
    }

    /**
     * 取出并校验账户标识。
     *
     * 长度上限 [MAX_ACCOUNT_LENGTH] 与 `economy_account.account` 列宽对齐：超长标识在
     * 插入时才被数据库拒绝会污染事务，这里提前 `require` 成可读的失败信息。
     */
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

    /**
     * `EconomyActionResult` → [LedgerOp]。
     *
     * 把 `EconomyCore` 的状态枚举原样放进 [LedgerOp.message]，因此 `BaseLedger` 的失败码
     * 就是 `INVALID_AMOUNT` / `INSUFFICIENT_BALANCE` 这些枚举名，与 mirai 实现的手写码一致。
     */
    private fun toOp(result: EconomyActionResult): LedgerOp =
        LedgerOp(result.success, result.balance?.value, result.status.name)

    /** 只读查询的统一入口：单事务 + `isDefaultReadOnly`，避免查询期间触发脏写。 */
    private fun <T> read(block: (Session) -> T): T =
        HibernateFactory.getSessionFactory().fromTransaction { session ->
            session.isDefaultReadOnly = true
            block(session)
        }
}
