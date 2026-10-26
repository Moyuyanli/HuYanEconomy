package cn.chahuyun.economy.games.raffle

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次抽奖指令的「抽奖人 + 场景」快照。
 *
 * @property user 抽奖人（群成员对象）。
 * @property subject 触发指令的群。
 * @property message 触发指令的消息链。
 */
data class RaffleActor(
    val user: Member,
    val subject: Group,
    val message: MessageChain,
)

/**
 * 抽奖功能的规则链 Provider 集合。
 */
object RaffleProvider {

    /** [RaffleActor] 的 Provider 限定名。 */
    const val ACTOR: String = "raffle-actor"

    /**
     * 加载本次抽奖指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): RaffleActor {
        val event = context.event as? GroupMessageEvent
            ?: error("抽奖动作只能由群消息触发，实际事件：${context.event::class.simpleName}")
        return RaffleActor(user = event.sender, subject = event.group, message = event.message)
    }
}
