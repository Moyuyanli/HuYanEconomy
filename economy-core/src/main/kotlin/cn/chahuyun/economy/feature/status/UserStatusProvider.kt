package cn.chahuyun.economy.feature.status

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次状态指令调用内的「成员 + 群」快照。
 *
 * @property userInfo 发送者资料（同一调用内共享）。
 * @property member 发送者群成员对象（扣款、权限判断用）。
 * @property group 触发指令的群（回复目标）。
 * @property message 触发指令的消息链（引用回复用）。
 */
data class UserStatusActor(
    val userInfo: UserInfoDto,
    val member: Member,
    val group: Group,
    val message: MessageChain,
)

/**
 * 用户状态功能的规则链 Provider 集合。
 *
 * 作用域为整个模块：`my` / `home` / `discharge` 三个动作都需要同一份快照。
 * 「回家 @QQ」的目标是别的成员，不复用本 Provider（见 [UserStatusUsecase.gotoHome]）。
 */
object UserStatusProvider {

    /** [UserStatusActor] 的 Provider 限定名。 */
    const val ACTOR: String = "status-actor"

    /**
     * 加载本次状态指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): UserStatusActor {
        val event = context.event as? GroupMessageEvent
            ?: error("状态动作只能由群消息触发，实际事件：${context.event::class.simpleName}")

        val sender = event.sender
        return UserStatusActor(
            userInfo = UserManager.getUserInfo(sender),
            member = sender,
            group = event.group,
            message = event.message,
        )
    }
}
