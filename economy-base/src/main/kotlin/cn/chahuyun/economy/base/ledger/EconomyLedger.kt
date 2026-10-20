package cn.chahuyun.economy.base.ledger

import java.math.BigDecimal

/**
 * 经济后端统一接口。
 *
 * 业务代码只依赖本接口，[BaseLedger]（本插件自建账户）与 `MiraiEconomyLedger`
 * （上游 mirai-economy 插件）是它的两个实现，由 `EconomyLedgerProvider` 在启动时
 * 按配置与依赖探测结果选定。
 *
 * 实现约定：
 * - 金额统一使用 [BigDecimal]，实现内部负责与自身存储精度对齐；
 * - 所有方法都必须自身异常安全，不把数据库 / 上游异常抛给业务层；
 * - 不得在方法间共享 Session、上下文等生命周期对象。
 */
interface EconomyLedger {

    /** 后端标识，用于日志与 `hye economy status` 展示。 */
    val backendId: String

    /** 启动自检：准备账户空间（Scope）、注册货币、启动缓存任务。失败应抛出异常由上层降级。 */
    fun start()

    /** 释放资源；不保证可重入。 */
    fun stop()

    /** 读取余额；账户不存在时按后端语义创建零余额账户。 */
    fun balance(owner: LedgerOwner): BigDecimal

    /** 增加余额；[amount] 必须为正数。 */
    fun add(owner: LedgerOwner, amount: BigDecimal): LedgerOp

    /** 扣减余额；[amount] 必须为正数，余额不足返回失败。 */
    fun subtract(owner: LedgerOwner, amount: BigDecimal): LedgerOp

    /** 在两个位置之间转账，必须原子完成。 */
    fun transfer(from: LedgerOwner, to: LedgerOwner, amount: BigDecimal): LedgerOp

    /** 按分类统计总额；无记录时为 0。 */
    fun total(book: LedgerBook): BigDecimal

    /** 枚举分类下的全部余额记录，用于迁移、排行与对账。 */
    fun entries(book: LedgerBook): List<LedgerEntry>

    /**
     * 带符号的增减：正数入账、负数扣款、0 直接成功。
     *
     * 上游旧接口（`plusMoneyTo*`）允许传负数表示扣款，为保持兼容语义由本方法统一处理。
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
        /** 构造用户钱包位置。 */
        fun wallet(qq: Long): LedgerOwner = LedgerOwner.Wallet("u$qq")

        /** 构造用户主银行位置。 */
        fun userBank(qq: Long): LedgerOwner = LedgerOwner.UserBank("u$qq")
    }
}
