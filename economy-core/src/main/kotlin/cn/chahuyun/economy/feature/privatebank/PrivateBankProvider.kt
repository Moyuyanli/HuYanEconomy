package cn.chahuyun.economy.feature.privatebank

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次私银指令调用内的「用户 + 场景」快照。
 *
 * @property userInfo 指令发送者资料（默认私银 code 从这里取）。
 * @property sender 发送者对象（服务层校验行长身份用）。
 * @property subject 回复目标（群或好友）。
 * @property message 触发指令的消息链（引用回复用）。
 * @property content 指令纯文本（参数解析用）。
 */
data class PrivateBankActor(
    val userInfo: UserInfoDto,
    val sender: User,
    val subject: Contact,
    val message: MessageChain,
    val content: String,
)

/**
 * 私银功能的规则链 Provider 集合。
 *
 * 私银的指令入口在 [PrivateBankUsecase]，对外查询入口在 [PrivateBankManager]。
 */
object PrivateBankProvider {

    /** [PrivateBankActor] 的 Provider 限定名。 */
    const val ACTOR: String = "privatebank-actor"

    /**
     * 加载本次私银指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): PrivateBankActor {
        val event = context.event as? MessageEvent
            ?: error("私银动作只能由消息事件触发，实际事件：${context.event::class.simpleName}")

        val sender = event.sender
        return PrivateBankActor(
            userInfo = UserManager.getUserInfo(sender),
            sender = sender,
            subject = event.subject,
            message = event.message,
            content = event.message.contentToString(),
        )
    }
}
