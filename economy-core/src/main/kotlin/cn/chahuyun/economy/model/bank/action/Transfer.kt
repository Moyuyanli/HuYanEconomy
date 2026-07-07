package cn.chahuyun.economy.model.bank.action

import cn.chahuyun.economy.utils.EconomyUtil
import net.mamoe.mirai.contact.User

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:51
 * 转帐功能
 */
class Transfer(originUser: User, private val toUser: User, private val money: Int) : BankAction() {

    init {
        this.user = originUser
    }

    @Throws(Exception::class)
    override fun execute() {
        val currentUser = user ?: throw Exception("用户不能为空")
        if (money <= 0) {
            throw Exception("不能为负")
        } else if (currentUser.id == toUser.id) {
            throw Exception("不能给自己转帐")
        } else if (EconomyUtil.getMoneyByUser(currentUser) < money) {
            throw Exception("余额不足")
        }
        EconomyUtil.turnUserToUser(currentUser, toUser, money.toDouble())
    }
}

