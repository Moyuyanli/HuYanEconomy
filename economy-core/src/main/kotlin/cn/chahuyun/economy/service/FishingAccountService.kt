package cn.chahuyun.economy.service

import cn.chahuyun.economy.base.ledger.LedgerOwner
import cn.chahuyun.economy.ledger.EconomyLedgerProvider
import java.math.BigDecimal

/**
 * 钓鱼玩法的资金适配层。
 *
 * 按 2.0 规范，钓鱼所有真实资金变动都通过当前经济后端完成，不再直接写
 * economy-base 的账户表——否则把后端切到 mirai-economy 后，钓鱼收入会落进另一套账本。
 *
 * 账户编码沿用 2.0 约定：
 * - 用户账户：`u<qq>`
 * - 鱼塘账户：`f<pondId>`（插件私有资金，与上游 custom 上下文的自定义账户对应）
 *
 * 业务表（`hye_user_pond`、`hye_fishing_log`）中的金额字段只作为统计、快照与展示投影，
 * 不得作为资金扣减或入账的最终依据。
 */
object FishingAccountService {

    private val ledger get() = EconomyLedgerProvider.current

    /** 读取用户账户余额。 */
    @JvmStatic
    fun userBalance(qq: Long): BigDecimal = ledger.balance(wallet(qq))

    /** 读取鱼塘账户余额。 */
    @JvmStatic
    fun pondBalance(pondId: Long): BigDecimal = ledger.balance(pond(pondId))

    /** 从用户账户扣除金额；余额不足返回 false。 */
    @JvmStatic
    fun deductUser(qq: Long, amount: BigDecimal): Boolean = ledger.subtract(wallet(qq), amount).success

    /** 向用户账户入账（负数表示扣减，允许负余额以兼容历史结算逻辑）。 */
    @JvmStatic
    fun addUser(qq: Long, amount: BigDecimal): Boolean = ledger.adjust(wallet(qq), amount).success

    /** 向鱼塘账户入账（负数表示扣减）。 */
    @JvmStatic
    fun addPond(pondId: Long, amount: BigDecimal): Boolean = ledger.adjust(pond(pondId), amount).success

    private fun wallet(qq: Long): LedgerOwner = LedgerOwner.Wallet("u$qq")

    private fun pond(pondId: Long): LedgerOwner = LedgerOwner.PluginAccount("f$pondId", "鱼塘资金")
}
