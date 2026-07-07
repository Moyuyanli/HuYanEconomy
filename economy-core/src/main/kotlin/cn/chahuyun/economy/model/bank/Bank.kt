package cn.chahuyun.economy.model.bank

import cn.chahuyun.economy.model.bank.action.BankAction

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:55
 */
class Bank private constructor() : AbstractBank() {
    companion object {
        @JvmField
        val INSTANCE = Bank()
    }

    @Throws(Exception::class)
    fun execute(action: BankAction) {
        action.execute()
    }
}

