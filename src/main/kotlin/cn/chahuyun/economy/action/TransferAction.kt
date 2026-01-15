package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.bank.Bank
import cn.chahuyun.economy.model.bank.action.Transfer
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.SingleMessage

/**
 * 转账管理
 * 转账 | 作弊
 */
@EventComponent
class TransferAction {

    /**
     * 用户转账给另一个用户操作
     */
    @MessageAuthorize(
        text = ["转账(\\[mirai:at:\\d+])? \\d+( \\d+)?"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun userToUser(event: MessageEvent) {
        Log.info("转账指令")

        val subject: Contact = event.subject
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user

        val message: MessageChain = event.message
        val code = message.serializeToMiraiCode()

        val s = code.split(" ")
        var qq = 0L
        val money: Double
        if (s.size == 2) {
            for (singleMessage: SingleMessage in message) {
                if (singleMessage is At) {
                    qq = singleMessage.target
                }
            }
            money = s.last().toDouble()
        } else {
            qq = s[1].toLong()
            money = s[2].toDouble()
        }

        if (money < 0 || user.id == qq) {
            subject.sendMessage("耍我了？小子？")
            return
        }

        val group = subject as? Group
        if (group == null || qq == 0L) {
            subject.sendMessage("转账失败！")
            return
        }

        val member = group[qq]
        if (member == null) {
            subject.sendMessage("转账失败！")
            return
        }

        val chainBuilder = MessageChainBuilder()
        if (EconomyUtil.turnUserToUser(user, member, money)) {
            chainBuilder.append(String.format("成功向%s转账%s金币", member.nick, money))
            subject.sendMessage(chainBuilder.build())
        } else {
            subject.sendMessage("转账失败！请联系管理员!")
            Log.error("转账管理:用户金币转移失败")
        }
    }

    companion object {
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

    @MessageAuthorize(
        text = ["greedisgood \\d+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun cheat(event: MessageEvent) {
        Log.info("作弊指令")

        val subject: Contact = event.subject
        val userInfo: UserInfo = UserCoreManager.getUserInfo(event.sender)
        val user = userInfo.user
        val message: MessageChain = event.message
        val code = message.serializeToMiraiCode()

        val s = code.split(" ")
        val money = s.last().toDouble()

        val chainBuilder = MessageChainBuilder()
        if (EconomyUtil.Cheat(user, money)) {
            chainBuilder.append(String.format("成功作弊: 获取%s金币", money))
            subject.sendMessage(chainBuilder.build())
        } else {
            subject.sendMessage("转账失败！请联系管理员!")
            Log.error("转账管理:用户金币转移失败")
        }
    }
}
