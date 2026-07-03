package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.utils.EconomyUtil
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import xyz.cssxsh.mirai.economy.EconomyService
import xyz.cssxsh.mirai.economy.service.EconomyAccount
import java.util.*

object FundingUsecase {

    suspend fun fundBind(event: FriendMessageEvent) {
        val message: MessageChain = event.message
        val content = message.contentToString()

        val qqId = content.split(" ")[2]
        val user = UserCoreManager.getUserInfo(qqId.toLong())

        if (user == null) {
            event.sender.sendMessage("未找到该用户")
            return
        }

//        if (user.funding != null) {
//            event.sender.sendMessage("该用户已绑定")
//            return
//        }

        val uuid = UUID.randomUUID().toString()
        user.funding = uuid
        UserCoreManager.saveUserInfo(user)

        event.sender.sendMessage("fund bind $qqId $uuid")
    }

    suspend fun fundGet(event: FriendMessageEvent) {
        val message: MessageChain = event.message
        val content = message.contentToString()

        val uuid = content.split(" ")[2]
        val user = UserCoreManager.getUserInfo(uuid)

        if (user == null) {
            event.sender.sendMessage("未找到该用户")
            return
        }

        val amount = content.split(" ")[3].toInt()

        val userId = user.id
        val account: EconomyAccount = EconomyService.account(userId, null)

        if (EconomyUtil.plusMoneyToBankForAccount(account, -amount.toDouble())) {
            event.sender.sendMessage("fund get $uuid $amount success")
        } else {
            event.sender.sendMessage("fund get $uuid $amount fail")
        }
    }
}

