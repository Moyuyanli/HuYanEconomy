package cn.chahuyun.economy.feature.shop

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次道具商店指令调用内的「买家 + 场景」快照。
 *
 * @property userInfo 买家资料（背包与余额都挂在它上面）。
 * @property sender 买家对象（扣款用）。
 * @property group 所在群（商店只在群聊可用）。
 * @property message 触发指令的消息链（引用回复用）。
 * @property content 指令纯文本（页码与购买清单解析用）。
 */
data class ShopActor(
    val userInfo: UserInfoDto,
    val sender: User,
    val group: Group,
    val message: MessageChain,
    val content: String,
)

/**
 * 道具商店功能的规则链 Provider 集合。
 */
object ShopProvider {

    /** [ShopActor] 的 Provider 限定名。 */
    const val ACTOR: String = "shop-actor"

    /**
     * 加载本次商店指令的参与者快照。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): ShopActor {
        val event = context.event as? GroupMessageEvent
            ?: error("道具商店动作只能由群消息触发，实际事件：${context.event::class.simpleName}")

        return ShopActor(
            userInfo = UserManager.getUserInfo(event.sender),
            sender = event.sender,
            group = event.group,
            message = event.message,
            content = event.message.contentToString(),
        )
    }
}
