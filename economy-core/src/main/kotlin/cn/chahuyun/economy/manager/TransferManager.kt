package cn.chahuyun.economy.manager

import cn.chahuyun.economy.model.bank.Bank
import cn.chahuyun.economy.model.bank.action.Transfer
import net.mamoe.mirai.contact.User

/**
 * 转账对外 API。
 */
object TransferManager {

    @JvmStatic
    fun transfer(originUser: User, toUser: User, money: Int): String {
        return try {
            Bank.INSTANCE.execute(Transfer(originUser, toUser, money))
            "转帐成功"
        } catch (e: Exception) {
            e.message ?: "转帐失败"
        }
    }
}
