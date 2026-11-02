package cn.chahuyun.economy.feature.privatebank

import cn.chahuyun.economy.ledger.EconomyProxy
import net.mamoe.mirai.contact.User

/**
 * 私人银行账本：把「银行 code + 资金池」翻译成插件私有账户。
 *
 * 账户标识规则（[accountId]）保持不变，读写统一走 [EconomyProxy]，金额精度由代理层抹平；
 * 本类只负责资金池语义与失败补偿。
 */
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

    /** 资金池账户句柄；[description] 只作展示，账户身份由 [accountId] 决定。 */
    fun account(bankCode: String, description: String): EconomyProxy.Account =
        EconomyProxy.plugin(accountId(bankCode, description), description)

    fun balance(bankCode: String, description: String): Double {
        return account(bankCode, description).balanceDouble()
    }

    fun add(bankCode: String, description: String, amount: Double): Boolean {
        return account(bankCode, description).adjust(amount).success
    }

    fun debit(bankCode: String, description: String, amount: Double): Boolean {
        if (amount <= 0) return false
        if (balance(bankCode, description) + 0.0001 < amount) return false
        return add(bankCode, description, -amount)
    }

    fun transferFromMainBank(user: User, bankCode: String, description: String, amount: Double): Boolean {
        return EconomyProxy.userBank(user).transferTo(account(bankCode, description), amount).success
    }

    fun transferFromWallet(user: User, bankCode: String, description: String, amount: Double): Boolean {
        if (amount <= 0) return false
        if (!EconomyProxy.wallet(user).debit(amount).success) return false
        if (add(bankCode, description, amount)) return true
        EconomyProxy.wallet(user).credit(amount)
        return false
    }

    fun transferToWallet(bankCode: String, description: String, user: User, amount: Double): Boolean {
        if (amount <= 0) return false
        if (!debit(bankCode, description, amount)) return false
        if (EconomyProxy.wallet(user).credit(amount).success) return true
        add(bankCode, description, amount)
        return false
    }

    fun transferToMainBank(bankCode: String, description: String, user: User, amount: Double): Boolean {
        if (amount <= 0) return false
        if (!debit(bankCode, description, amount)) return false
        if (EconomyProxy.userBank(user).adjust(amount).success) return true
        add(bankCode, description, amount)
        return false
    }
}
