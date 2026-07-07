package cn.chahuyun.economy.model.bank.action

import net.mamoe.mirai.contact.User

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:53
 * 存款功能
 */
class TopUp(user: User, var money: Int = 0) : BankAction(user) {
    override fun execute() {
        // 实现逻辑
    }
}
