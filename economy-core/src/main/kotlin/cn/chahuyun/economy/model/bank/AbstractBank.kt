package cn.chahuyun.economy.model.bank

import cn.chahuyun.economy.model.bank.action.BankAction
import net.mamoe.mirai.contact.User

/**
 * 银行操作上下文基类。
 *
 * user 表示当前操作的 mirai 用户，bankAction 表示本次要执行的存取/转账动作。
 * 旧版 Java API 仍会直接构造该类型，因此保持 open class 与可空字段。
 *
 * @author Erzbir
 * @Date: 2022/11/29 22:11
 */
open class AbstractBank(
    var user: User? = null,
    var bankAction: BankAction? = null
)
