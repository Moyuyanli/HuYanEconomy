package cn.chahuyun.economy.service

import cn.chahuyun.economy.base.EconomyAccount
import cn.chahuyun.economy.base.EconomyAction
import cn.chahuyun.economy.base.EconomyBalance
import java.math.BigDecimal

/**
 * 钓鱼玩法的资金适配层。
 *
 * 按 2.0 规范，钓鱼所有真实资金变动都通过 economy-base 的账户完成：
 * - 用户账户编码：`u<qq>`（Scope DEFAULT）
 * - 鱼塘账户编码：`f<pondId>`（Scope DEFAULT）
 *
 * 业务表（`hye_user_pond`、`hye_fishing_log`）中的金额字段只作为统计、快照与展示投影，
 * 不得作为资金扣减或入账的最终依据。
 */
object FishingAccountService {

    /** 用户个人账户。 */
    @JvmStatic
    fun userAccount(qq: Long): EconomyAccount = EconomyAccount.take("u$qq")

    /** 鱼塘资金池账户。 */
    @JvmStatic
    fun pondAccount(pondId: Long): EconomyAccount = EconomyAccount.take("f$pondId")

    /** 读取用户账户余额。 */
    @JvmStatic
    fun userBalance(qq: Long): BigDecimal = userAccount(qq).balance.value

    /** 读取鱼塘账户余额。 */
    @JvmStatic
    fun pondBalance(pondId: Long): BigDecimal = pondAccount(pondId).balance.value

    /** 从用户账户扣除金额。 */
    @JvmStatic
    fun deductUser(qq: Long, amount: BigDecimal): Boolean =
        EconomyAction.subtract(userAccount(qq), EconomyBalance.of(amount)).success

    /** 向用户账户入账。 */
    @JvmStatic
    fun addUser(qq: Long, amount: BigDecimal): Boolean =
        EconomyAction.add(userAccount(qq), EconomyBalance.of(amount)).success

    /** 向鱼塘账户入账。 */
    @JvmStatic
    fun addPond(pondId: Long, amount: BigDecimal): Boolean =
        EconomyAction.add(pondAccount(pondId), EconomyBalance.of(amount)).success
}
