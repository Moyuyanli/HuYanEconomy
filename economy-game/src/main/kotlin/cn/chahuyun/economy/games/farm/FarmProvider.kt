package cn.chahuyun.economy.games.farm

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次农场指令的「农场主 + 场景」快照。
 *
 * 农场十八个动作全部是群消息触发，因此这里直接持有 [Group]，
 * 供 `帮浇水` / `偷菜` 这类跨用户玩法解析目标成员。
 *
 * 这里**不加载** `UserInfoDto`：查看、商店、仓库这些动作完全不需要用户资料，
 * 提前查库会让「看农场」也产生一次用户建档。真正需要资料的 `water` / `steal`
 * 在动作里按需调用 `EconomyUserService.getOrCreate`。
 *
 * @property user 农场主（群成员对象）。
 * @property subject 触发指令的群。
 * @property message 触发指令的消息链（播种/收获的土地编号从这条消息里解析）。
 */
data class FarmActor(
    val user: Member,
    val subject: Group,
    val message: MessageChain,
)

/**
 * 农场功能的规则链 Provider 集合。
 */
object FarmProvider {

    /** [FarmActor] 的 Provider 限定名。 */
    const val ACTOR: String = "farm-actor"

    /**
     * 加载本次农场指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): FarmActor {
        val event = context.event as? GroupMessageEvent
            ?: error("农场动作只能由群消息触发，实际事件：${context.event::class.simpleName}")
        return FarmActor(user = event.sender, subject = event.group, message = event.message)
    }
}
