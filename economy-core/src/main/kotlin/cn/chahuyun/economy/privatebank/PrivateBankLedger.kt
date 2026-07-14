package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.utils.EconomyUtil
import net.mamoe.mirai.contact.User

object PrivateBankLedger {
    const val RESERVE_DESC = "pb-reserve"
    const val LIQUIDITY_DESC = "pb-liquidity"
    const val INVENTORY_DESC = "pb-inventory"

    fun accountId(bankCode: String, description: String): String {
        return when (description) {
            RESERVE_DESC -> "$bankCode-P"
            LIQUIDITY_DESC -> "$bankCode-F"
            INVENTORY_DESC -> "$bankCode-L"
            else -> bankCode
        }
    }

    fun balance(bankCode: String, description: String): Double {
        return EconomyUtil.getMoneyFromPluginBankForId(accountId(bankCode, description), description)
    }

    fun add(bankCode: String, description: String, amount: Double): Boolean {
        return EconomyUtil.plusMoneyToPluginBankForId(accountId(bankCode, description), description, amount)
    }

    fun debit(bankCode: String, description: String, amount: Double): Boolean {
        if (amount <= 0) return false
        if (balance(bankCode, description) + 0.0001 < amount) return false
        return add(bankCode, description, -amount)
    }

    fun transferFromMainBank(user: User, bankCode: String, description: String, amount: Double): Boolean {
        return EconomyUtil.turnUserGlobalBankToPluginBankForId(user, accountId(bankCode, description), description, amount)
    }

    fun transferFromWallet(user: User, bankCode: String, description: String, amount: Double): Boolean {
        if (amount <= 0) return false
        if (!EconomyUtil.minusMoneyToUser(user, amount)) return false
        if (add(bankCode, description, amount)) return true
        EconomyUtil.plusMoneyToUser(user, amount)
        return false
    }

    fun transferToWallet(bankCode: String, description: String, user: User, amount: Double): Boolean {
        if (amount <= 0) return false
        if (!debit(bankCode, description, amount)) return false
        if (EconomyUtil.plusMoneyToUser(user, amount)) return true
        add(bankCode, description, amount)
        return false
    }

    fun transferToMainBank(bankCode: String, description: String, user: User, amount: Double): Boolean {
        if (amount <= 0) return false
        if (!debit(bankCode, description, amount)) return false
        if (EconomyUtil.plusMoneyToBank(user, amount)) return true
        add(bankCode, description, amount)
        return false
    }
}
