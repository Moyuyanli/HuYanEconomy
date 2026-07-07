package cn.chahuyun.economy.service

import cn.chahuyun.economy.utils.EconomyUtil
import net.mamoe.mirai.contact.User

/**
 * Core-facing account operations for feature modules.
 *
 * This is intentionally a thin facade over EconomyUtil while 2.0.0 keeps the
 * existing account semantics stable.
 */
object EconomyAccountService {

    @JvmStatic
    fun walletBalance(user: User): Double =
        EconomyUtil.getMoneyByUser(user)

    @JvmStatic
    fun addWallet(user: User, amount: Double): Boolean =
        EconomyUtil.plusMoneyToUser(user, amount)

    @JvmStatic
    fun subtractWallet(user: User, amount: Double): Boolean =
        EconomyUtil.minusMoneyToUser(user, amount)

    @JvmStatic
    fun pluginBankBalance(accountId: String, description: String): Double =
        EconomyUtil.getMoneyFromPluginBankForId(accountId, description)

    @JvmStatic
    fun addPluginBank(accountId: String, description: String, amount: Double): Boolean =
        EconomyUtil.plusMoneyToPluginBankForId(accountId, description, amount)
}
