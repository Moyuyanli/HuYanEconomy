package cn.chahuyun.economy.feature.bank

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次主银行指令调用内的「用户 + 场景」快照。
 *
 * 主银行不区分群聊/私聊：`Contact` 就是回复目标，好友消息与群消息共用同一套逻辑。
 *
 * @property userInfo 指令发送者资料（默认私银 code、账户标识都从这里取）。
 * @property user 发送者对象（账本扣款、余额查询用）。
 * @property subject 回复目标（群或好友）。
 * @property message 触发指令的消息链（引用回复用）。
 * @property content 指令纯文本（路由与金额解析用）。
 */
data class BankActor(
    val userInfo: UserInfoDto,
    val user: User,
    val subject: Contact,
    val message: MessageChain,
    val content: String,
)

/**
 * 主银行功能的规则链 Provider 集合。
 */
object BankProvider {

    /** [BankActor] 的 Provider 限定名。 */
    const val ACTOR: String = "bank-actor"

    /**
     * 加载本次主银行指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): BankActor {
        val event = context.event as? MessageEvent
            ?: error("主银行动作只能由消息事件触发，实际事件：${context.event::class.simpleName}")

        val sender = event.sender
        return BankActor(
            userInfo = UserManager.getUserInfo(sender),
            user = sender,
            subject = event.subject,
            message = event.message,
            content = event.message.contentToString(),
        )
    }
}
