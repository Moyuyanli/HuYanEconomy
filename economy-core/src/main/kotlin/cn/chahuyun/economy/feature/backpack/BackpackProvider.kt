package cn.chahuyun.economy.feature.backpack

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次背包指令调用内的「成员 + 背包」快照。
 *
 * `userInfo.backpacks` 是本次操作的唯一数据源：查看时直接分页，使用时按 `propId` 匹配，
 * 丢弃时删完即从列表移除——三个阶段共享同一实例，避免重复查库导致「刚丢的道具还在」。
 *
 * @property userInfo 发送者资料（含背包列表）。
 * @property member 发送者群成员对象（使用道具时作为 [cn.chahuyun.economy.model.props.UseEvent] 的执行者）。
 * @property group 触发指令的群（回复与发图目标）。
 * @property message 触发指令的消息链（引用回复、解析道具 ID 用）。
 */
data class BackpackActor(
    val userInfo: UserInfoDto,
    val member: Member,
    val group: Group,
    val message: MessageChain,
)

/**
 * 背包功能的规则链 Provider 集合。
 *
 * 背包被签到（扣消耗道具）、称号/道具效果（医保卡判定）等功能读取，
 * 对外契约由 [BackpackManager] 承担；这里只放运行时快照。
 */
object BackpackProvider {

    /** [BackpackActor] 的 Provider 限定名。 */
    const val ACTOR: String = "backpack-actor"

    /**
     * 加载本次背包指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): BackpackActor {
        val event = context.event as? GroupMessageEvent
            ?: error("背包动作只能由群消息触发，实际事件：${context.event::class.simpleName}")

        val sender = event.sender
        return BackpackActor(
            userInfo = UserManager.getUserInfo(sender),
            member = sender,
            group = event.group,
            message = event.message,
        )
    }
}
