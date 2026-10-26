package cn.chahuyun.economy.feature.title

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次称号指令调用内的「用户 + 场景」快照。
 *
 * 称号的四个动作都要「查一次用户资料、回复到同一个会话」；统一由
 * [TitleProvider.actor] 加载，避免动作与前置规则各查一遍库、拿到不同实例。
 *
 * @property userInfo 用户资料（同一调用内共享，可由前置规则原地补充）。
 * @property user 指令发送者（扣款、余额查询用）。
 * @property subject 回复目标（群或好友）。
 * @property message 触发指令的消息链（解析称号名、引用回复用）。
 */
data class TitleActor(
    val userInfo: UserInfoDto,
    val user: User,
    val subject: Contact,
    val message: MessageChain,
)

/**
 * 称号功能的规则链 Provider 集合。
 *
 * 称号既有本功能内的动作（查看/购买/切换），也被签到（`@After` 检查连签称号）和
 * 个人资料卡（默认称号渲染）使用，因此对外契约由 [TitleManager] 承担；
 * 这里只放「一次调用取一次」的运行时数据。
 */
object TitleProvider {

    /** [TitleActor] 的 Provider 限定名；注解与规则参数共用这一个常量。 */
    const val ACTOR: String = "title-actor"

    /**
     * 加载本次称号指令的参与者快照。
     *
     * 作用域为整个模块（不限定 actionId）：称号的查看、商店、购买、切换四个动作
     * 都需要同一份快照。加载失败直接抛异常——属于接线问题，应在联调期暴露。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     */
    @Provider(qualifier = ACTOR)
    suspend fun actor(context: ActionContext): TitleActor {
        val event = context.event as? MessageEvent
            ?: error("称号动作只能由消息事件触发，实际事件：${context.event::class.simpleName}")

        val sender = event.sender
        return TitleActor(
            userInfo = UserManager.getUserInfo(sender),
            user = sender,
            subject = event.subject,
            message = event.message,
        )
    }
}
