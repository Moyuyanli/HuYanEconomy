package cn.chahuyun.economy.games.rob

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次抢劫指令的「发起人 + 场景」快照。
 *
 * 抢劫是纯群聊玩法，四个动作（抢劫/打人/开启/关闭）都只可能由群消息触发，
 * 因此这里直接持有 [Group]，供权限组操作使用。
 *
 * @property user 发起人（群成员对象）。
 * @property subject 触发指令的群。
 * @property message 触发指令的消息链。
 */
data class RobActor(
    val user: Member,
    val subject: Group,
    val message: MessageChain,
)

/**
 * 抢劫功能的规则链 Provider 集合。
 */
object RobProvider {

    /** [RobActor] 的 Provider 限定名。 */
    const val ACTOR: String = "rob-actor"

    /**
     * 加载本次抢劫指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): RobActor {
        val event = context.event as? GroupMessageEvent
            ?: error("抢劫动作只能由群消息触发，实际事件：${context.event::class.simpleName}")
        return RobActor(user = event.sender, subject = event.group, message = event.message)
    }
}
