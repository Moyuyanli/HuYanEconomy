package cn.chahuyun.economy.feature.sign

import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.feature.sign.SignProvider.signer
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 一次签到调用内的「参与者 + 场景」快照。
 *
 * **为什么要单独抽一个类型**：签到的前置规则（资格校验、奖励计算）、动作本体、后置规则
 * （日志、称号检查）都要用到同一份用户信息与同一条群消息。如果各写各的
 * `UserCoreManager.getUserInfo(event.sender)`，一次指令会查三遍库，而且拿到的不是同一个实例——
 * `@Before` 里补签卡改过的连签数，动作本体就读不到。
 *
 * 由 [SignProvider.signer] 统一定义，规则参数写 `@FromProvider("signer") actor: SignActor` 即可注入；
 * 同一次调用内只加载一次（框架按「类型 + 限定名」缓存）。
 *
 * @property userInfo 用户信息快照；**同一次调用内各阶段共享同一个实例**，可原地修改。
 * @property user 本次指令的发送者（发钱、查余额用）。
 * @property subject 消息发送目标（群），用于回消息与发图。
 * @property message 触发指令的消息链，用于引用回复与事件文案。
 * @property groupId 群号；单独存一份，避免后置规则再对事件做类型转换。
 */
data class SignActor(
    val userInfo: UserInfoDto,
    val user: User,
    val subject: Contact,
    val message: MessageChain,
    val groupId: Long,
)

/**
 * 签到功能的规则链 Provider 集合。
 *
 * 只放「一次调用内取一次、多处复用」的运行时数据；资格判定、奖励计算等纯计算都放在
 * [SignService]（对内），签到没有对外调用方，因此本功能不提供 Manager。
 */
object SignProvider {

    /**
     * `SignActor` 的 Provider 限定名。
     *
     * 注解与代码侧共用这一个常量：[signer] 上写 `@Provider(qualifier = SIGNER)`，
     * 规则参数写 `@FromProvider(SIGNER)` 即可，不要在两处各写一遍字符串。
     *
     * ⚠️ KSP 读注解参数的方式是 `arguments.firstOrNull { it.name == "qualifier" }?.value as? String`，
     * **只有编译期常量会被解析成字符串**。`const val` 已实测可用（生成物里就是 `"signer"`）；
     * 换成普通 `val`、函数调用或字符串拼接，`value` 就不是 String，会被 `orEmpty()` 静默降级成空串，
     * 且**不会有任何编译错误**——届时 Provider 无法被 `@FromProvider("signer")` 命中。
     * 改这个值后务必打开 `EconomyCoreActionModule` 核对生成结果。
     */
    const val SIGNER: String = "signer"

    /**
     * 加载本次签到的参与者快照。
     *
     * 作用域限定在 `core.sign`（局部 Provider），签到之外的动作用不到；
     * 加载失败直接抛异常——这类错误属于接线问题，应该在装配/联调期暴露，
     * 而不是让玩家看到一条「签到失败」的模糊提示。
     *
     * @param context 本次调用上下文，从中取出触发事件。
     * @return 参与者快照（同一个调用内被缓存复用）。
     */
    @Provider(qualifier = SIGNER, actionId = "core.sign")
    suspend fun signer(context: ActionContext): SignActor {
        // 签到只可能由群消息触发；类型不符说明 actionId 被错误地从别处调用。
        val event = context.event as? GroupMessageEvent
            ?: error("core.sign 只能由群消息触发，实际事件：${context.event::class.simpleName}")

        val sender = event.sender
        return SignActor(
            // 用户信息在这里查一次，后续阶段共享同一实例（补签卡修改的连签数才能传递下去）。
            userInfo = UserCoreManager.getUserInfo(sender),
            user = sender,
            subject = event.subject,
            message = event.message,
            groupId = event.group.id,
        )
    }
}
