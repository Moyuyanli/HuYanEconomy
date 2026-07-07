package cn.chahuyun.economy.model.bank

import cn.chahuyun.economy.model.bank.action.BankAction
import net.mamoe.mirai.contact.User

/**
 * @author Erzbir
 * @Date: 2022/11/29 22:11
 */
open class AbstractBank(
    var user: User? = null,
    var bankAction: BankAction? = null
)
