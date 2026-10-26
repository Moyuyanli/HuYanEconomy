package cn.chahuyun.economy.feature.status

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.constant.UserLocation
import cn.chahuyun.economy.feature.backpack.BackpackManager
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.model.props.PropsCard
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply
import java.util.*

/**
 * 用户状态功能的主用例：位置 / 回家 / 出院。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `core.status.my` | 我的状态 / 我的位置 | 无（纯查询） |
 * | `core.status.home` | 回家 | `@Before` 位置校验（已在家 / 在医院或监狱） |
 * | `core.status.homeAt` | 回家 @QQ（管理员） | 无 |
 * | `core.status.discharge` | 出院 | `@Before` 是否住院校验；`@After` 出院日志 |
 *
 * 「出院」原先挂在 `UserAction` 的指令上、逻辑写在 `UserUsecase.discharge`，
 * 按归属（改的是位置状态、扣的是医药费）整体迁到本功能，指令层入口随之改指。
 */
object UserStatusUsecase {

    /**
     * 查看自己当前的位置状态（`core.status.my`）。
     *
     * @param event 群消息事件。
     * @param context 本次调用上下文，读取 [UserStatusActor]。
     */
    @Action("core.status.my")
    suspend fun myStatus(event: GroupMessageEvent, context: ActionContext) {
        Log.info("我的状态:群(${event.group.id}) 用户(${event.sender.id})")
        val actor = context.provide(UserStatusActor::class, UserStatusProvider.ACTOR)
        val message: MessageChain = actor.message
        val group: Group = actor.group

        val userStatus = UserStatusManager.getUserStatus(actor.userInfo)

        when (userStatus.place) {
            UserLocation.HOME.name -> {
                group.sendMessage(MessageUtil.formatMessageChain(message, "你现在正在家里躺着哩~"))
                return
            }

            UserLocation.PRISON.name -> {
                val time = userStatus.recoveryTime
                val between = DateUtil.between(Date(userStatus.startTime), Date(), DateUnit.MINUTE)
                group.sendMessage(
                    MessageUtil.formatMessageChain(message, "你还在监狱，剩余拘禁时间:${time - between}分钟")
                )
                return
            }

            UserLocation.HOSPITAL.name -> {
                group.sendMessage(MessageUtil.formatMessageChain(message, "你现在正在医院躺着，wifi速度还行."))
                return
            }

            UserLocation.FACTORY.name -> {
                group.sendMessage(MessageUtil.formatMessageChain(message, "你现在正在工厂，这里很吵闹!"))
                return
            }

            UserLocation.FISHPOND.name -> {
                group.sendMessage(MessageUtil.formatMessageChain(message, "嘘，别把我的鱼吓跑了!"))
                return
            }

            else -> {}
        }
    }

    /**
     * 回家（`core.status.home`）。
     *
     * 「已经在家」「在医院/监狱不能回家」由 [UserStatusRules.validateGoHome] 前置拦截，
     * 动作只做状态推进与回复。
     *
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.status.home")
    suspend fun goHome(event: GroupMessageEvent, context: ActionContext) {
        Log.info("回家:群(${event.group.id}) 用户(${event.sender.id})")
        val actor = context.provide(UserStatusActor::class, UserStatusProvider.ACTOR)
        UserStatusManager.moveHome(actor.userInfo)
        actor.group.sendMessage(MessageUtil.formatMessageChain(actor.message, "你回家躺着去."))
    }

    /**
     * 让指定成员回家（`core.status.homeAt`，管理员）。
     *
     * 目标用户来自指令中的 @，不是发送者本人，因此不复用 [UserStatusActor]。
     *
     * @param event 群消息事件。
     */
    @Action("core.status.homeAt")
    suspend fun gotoHome(event: GroupMessageEvent) {
        val group: Group = event.group
        val message: MessageChain = event.message

        val member: Member? = ShareUtils.getAtMember(event)
        if (member == null) {
            Log.warning("该用户不存在!")
            return
        }

        val userInfo = UserManager.getUserInfo(member)
        UserStatusManager.moveHome(userInfo)
        group.sendMessage(MessageUtil.formatMessageChain(message, "你让ta回家躺着去."))
    }

    /**
     * 出院（`core.status.discharge`）。
     *
     * 医药费按「剩余恢复时间 × 3」计算，持有医保卡打八折；扣款成功才把位置改回家里。
     * 是否住院由 [UserStatusRules.validateDischarge] 前置判定。
     *
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.status.discharge")
    suspend fun discharge(event: GroupMessageEvent, context: ActionContext) {
        Log.info("出院:群(${event.group.id}) 用户(${event.sender.id})")
        val actor = context.provide(UserStatusActor::class, UserStatusProvider.ACTOR)
        val userInfo = actor.userInfo
        val user: Member = actor.member
        val group: Group = actor.group

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(actor.message))

        val userStatus = UserStatusManager.getUserStatus(userInfo)
        val price = userStatus.recoveryTime.toDouble() * 3.0
        val real: Double

        if (BackpackManager.checkPropInUser(userInfo, PropsCard.HEALTH)) {
            real = ShareUtils.rounding(price * 0.8)
            builder.add(
                MessageUtil.formatMessage(
                    "你在出院的时候使用了医保卡，医药费打8折。\n" +
                        "原价/实付医药费:${MoneyFormatUtil.format(price)}/${MoneyFormatUtil.format(real)}"
                )
            )
        } else {
            real = price
            builder.add(MessageUtil.formatMessage("你出院了！这次只掏了${MoneyFormatUtil.format(real)}的医药费！"))
        }

        if (EconomyUtil.minusMoneyToUser(user, real)) {
            group.sendMessage(builder.build())
            UserStatusManager.moveHome(userInfo)
        } else {
            group.sendMessage("出院失败!")
        }
    }
}
