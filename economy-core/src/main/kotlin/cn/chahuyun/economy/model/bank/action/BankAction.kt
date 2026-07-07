package cn.chahuyun.economy.model.bank.action

import net.mamoe.mirai.contact.User

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:55
 * 银行所有操作的父类
 */
abstract class BankAction(
    var user: User? = null // 一个操作绑定一个用户
) {
    @Throws(Exception::class)
    abstract fun execute()
}
