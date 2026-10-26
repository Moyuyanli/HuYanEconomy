package cn.chahuyun.economy.games.lottery

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次猜签指令的「投注人 + 场景」快照。
 *
 * @property user 投注人（群成员对象）。
 * @property subject 触发指令的群。
 * @property message 触发指令的消息链（投注号码与金额都从这条消息里解析）。
 */
data class LotteryActor(
    val user: Member,
    val subject: Group,
    val message: MessageChain,
)

/**
 * 猜签功能的规则链 Provider 集合。
 */
object LotteryProvider {

    /** [LotteryActor] 的 Provider 限定名。 */
    const val ACTOR: String = "lottery-actor"

    /**
     * 加载本次猜签指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): LotteryActor {
        val event = context.event as? GroupMessageEvent
            ?: error("猜签动作只能由群消息触发，实际事件：${context.event::class.simpleName}")
        return LotteryActor(user = event.sender, subject = event.group, message = event.message)
    }
}
