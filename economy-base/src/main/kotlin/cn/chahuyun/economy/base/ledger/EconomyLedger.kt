package cn.chahuyun.economy.base.ledger

import cn.chahuyun.economy.base.ledger.EconomyLedger.Companion.wallet
import java.math.BigDecimal

/**
 * 经济后端统一接口。
 *
 * 业务代码只依赖本接口，[BaseLedger]（本插件自建账户）与 `MiraiEconomyLedger`
 * （上游 mirai-economy 插件）是它的两个实现，由 `EconomyLedgerProvider` 在启动时
 * 按配置与依赖探测结果选定。
 *
 * 业务侧通常不直接调用本接口，而是经 `EconomyProxy` 的位置句柄进入：
 * `EconomyProxy.wallet(user).debit(money)` 最终落到 [subtract]。
 *
 * ## 实现约定（所有实现都必须满足）
 * 1. **金额统一 [BigDecimal]**：接口层不做舍入，实现内部负责与自身存储精度对齐；
 *    `Double` 的两位 HALF_UP 转换由代理的 `money()` 完成，本接口不接收 `Double`。
 * 2. **异常安全**：任何方法都不得把数据库 / 上游异常抛给业务层，失败一律转成
 *    [LedgerOp.fail]；查询类方法（[balance]、[total]、[entries]）失败时返回零值或空集合。
 * 3. **无状态句柄**：不得在方法之间共享 `Session`、上游 `context` 等生命周期对象，
 *    每次操作自行开闭。唯一允许的长期状态是 [start] 里准备好的只读元数据
 *    （如 `BaseLedger` 的 Scope 缓存）。
 * 4. **不抛业务异常**：[start] 是唯一允许抛异常的方法（由 Provider 捕获后决定降级），
 *    其余方法的失败信息通过 [LedgerOp.message] 返回。
 *
 * ## 失败信息约定（[LedgerOp.message]）
 * 业务侧可据此做出提示文案，实现方新增取值时必须同步《经济代理 API 参考》：
 *
 * | 取值 | 含义 | 产生位置 |
 * |------|------|----------|
 * | `INVALID_AMOUNT` | 金额非正数、超出 `DECIMAL(19,2)` 精度 | 各实现的 add/subtract/transfer |
 * | `INSUFFICIENT_BALANCE` | 余额不足（`debit` 不走负余额） | subtract / transfer（mirai） |
 * | `TRANSFER_ROLLBACK` | 转入失败，已回滚转出方 | mirai 的补偿式 transfer |
 * | `ACCOUNT_MISSING` | 目标账户记录不存在 | `BaseLedger` 强制扣款分支 |
 * | 其它文本 | 底层异常 message，仅用于日志排查 | 各实现的兜底 catch |
 *
 * ## 并发模型
 * 接口不承诺跨账户的串行化：单账户的读-改-写在实现内部保证原子（`BaseLedger` 用
 * 事务或悲观写锁，mirai 依赖上游自身的锁），跨账户组合操作需要调用方自行加业务锁。
 */
interface EconomyLedger {

    /** 后端标识，用于日志与 `hye economy status` 展示。 */
    val backendId: String

    /** 启动自检：准备账户空间（Scope）、注册货币、启动缓存任务。失败应抛出异常由上层降级。 */
    fun start()

    /**
     * 释放资源；不保证可重入。
     *
     * `BaseLedger` 只清空 Scope 缓存；mirai 实现刻意**不**关闭上游服务
     * （上游服务的生命周期由 mirai-economy 插件自己管理，关掉会影响其他插件）。
     */
    fun stop()

    /**
     * 读取余额；账户不存在时按后端语义创建零余额账户。
     *
     * 查询失败（表不可用、上游未就绪）不抛异常，返回 `BigDecimal.ZERO`——
     * 余额展示类调用不能因为底层抖动而让整个指令失败。
     */
    fun balance(owner: LedgerOwner): BigDecimal

    /** 增加余额；[amount] 必须为正数。 */
    fun add(owner: LedgerOwner, amount: BigDecimal): LedgerOp

    /** 扣减余额；[amount] 必须为正数，余额不足返回失败。 */
    fun subtract(owner: LedgerOwner, amount: BigDecimal): LedgerOp

    /** 在两个位置之间转账，必须原子完成。 */
    fun transfer(from: LedgerOwner, to: LedgerOwner, amount: BigDecimal): LedgerOp

    /** 按分类统计总额；无记录时为 0。口径包含负数余额（与旧 `getBankTotalCached()` 一致）。 */
    fun total(book: LedgerBook): BigDecimal

    /** 枚举分类下的全部余额记录，用于迁移、排行与对账。失败时返回空集合而非抛异常。 */
    fun entries(book: LedgerBook): List<LedgerEntry>

    /**
     * 带符号的增减：正数入账、负数扣款、0 直接成功。
     *
     * 上游旧接口（`plusMoneyTo*`）允许传负数表示扣款，为保持兼容语义由本方法统一处理。
     *
     * 默认实现只是把符号拆给 [add] / [subtract]，因此**负数分支同样受余额下限保护**。
     * 需要"允许透支"的强制扣款时，实现必须覆盖本方法并自己绕开下限
     * （`BaseLedger` 即覆盖为私有 `forceDebit`）；不要指望在这里拿到负余额。
     */
    fun adjust(owner: LedgerOwner, amount: BigDecimal): LedgerOp {
        val sign = amount.signum()
        return when {
            sign > 0 -> add(owner, amount)
            sign < 0 -> subtract(owner, amount.negate())
            else -> LedgerOp.ok(balance(owner))
        }
    }

    companion object {
        /**
         * 构造用户钱包位置。
         *
         * 只是 `LedgerOwner.Wallet("u$qq")` 的便捷写法，**不含任何后端语义**；
         * 业务代码请改用 `EconomyProxy.wallet(qq)`，以便走统一的账户编码入口。
         */
        fun wallet(qq: Long): LedgerOwner = LedgerOwner.Wallet("u$qq")

        /** 构造用户主银行位置。语义同 [wallet]：便捷构造，编码规则仍以代理的 `walletId` 为准。 */
        fun userBank(qq: Long): LedgerOwner = LedgerOwner.UserBank("u$qq")
    }
}
