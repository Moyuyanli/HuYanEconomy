package cn.chahuyun.economy.feature.user

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次用户指令调用内的「本人 + 场景」快照。
 *
 * @property userInfo 用户资料（同一调用内共享，前置规则修正称号后动作可见）。
 * @property member 发送者对象（头像、余额查询用）。
 * @property subject 回复目标（群或好友）。
 * @property message 触发指令的消息链。
 */
data class UserActor(
    val userInfo: UserInfoDto,
    val member: User,
    val subject: Contact,
    val message: MessageChain,
)

/**
 * 用户功能的规则链 Provider 集合。
 *
 * 本功能对外的资料读写契约在 [UserManager]（全项目 19 处引用），
 * 这里只提供「一次调用取一次」的运行时快照。
 */
object UserProvider {

    /** [UserActor] 的 Provider 限定名。 */
    const val ACTOR: String = "user-actor"

    /**
     * 加载本次用户指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): UserActor {
        val event = context.event as? MessageEvent
            ?: error("用户动作只能由消息事件触发，实际事件：${context.event::class.simpleName}")

        val sender = event.sender
        return UserActor(
            userInfo = UserManager.getUserInfo(sender),
            member = sender,
            subject = event.subject,
            message = event.message,
        )
    }
}
