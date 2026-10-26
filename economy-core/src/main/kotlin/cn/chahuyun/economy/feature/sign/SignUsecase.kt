package cn.chahuyun.economy.feature.sign

import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.feature.backpack.BackpackManager
import cn.chahuyun.economy.feature.sign.SignUsecase.sign
import cn.chahuyun.economy.feature.user.UserManager
import cn.chahuyun.economy.ledger.UserWallet
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.service.EconomyAsyncService
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateTime
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import java.awt.image.BufferedImage
import java.math.BigDecimal
import java.util.*

/**
 * 一次签到主流程的结果快照。
 *
 * 由 [SignUsecase.sign] 作为**返回值**交出，引擎写进 `ActionContext`，
 * `@After` 规则（[SignRules.logCommitted] / [SignRules.grantSignTitles]）用
 * `context.actionResult(SignOutcome::class)` 读取。
 *
 * 主流程提前结束（已签到、入账失败）时返回 `null`，后置规则据此跳过——这比原来
 * 广播 `SignCommittedEvent` 更明确：返回值就是「这次到底成没成」的唯一判据。
 *
 * @property qq 签到用户 QQ。
 * @property gold 本次实发金币（已含倍率），已按账本精度舍入到两位小数。
 * @property signNumber 本次签到后的连签天数。
 * @property groupId 触发签到的群号。
 */
data class SignOutcome(
    val qq: Long,
    val gold: BigDecimal,
    val signNumber: Int,
    val groupId: Long,
)

/**
 * 签到功能的主用例：本功能对外暴露的**全部动作**都收在这里。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `core.sign` | 签到 / 打卡 / sign | `@Before` 资格校验 → 奖励计算；`@After` 完成日志 → 称号检查 |
 * | `core.sign.close` | 关闭 签到（管理员） | `@After` 审计日志 |
 * | `core.sign.open` | 开启 签到（管理员） | `@After` 审计日志 |
 * | `core.sign.refresh` | 刷新签到（管理员） | `@After` 审计日志 |
 *
 * 四个动作都由 KSP 生成的门面 `SignUsecaseActions` 暴露给 `economy-main` 的指令层，
 * 指令层不关心规则链细节，只负责「指令 → 动作」的映射。
 *
 * ## 主流程与规则的分工
 * - 能不能签、这次给多少 → [SignRules] 的前置规则（可拦截、可扩展）；
 * - 推进状态、入账、扣道具、落库、发图 → 动作本体（不可跳过的一串副作用）；
 * - 签完之后的日志与称号 → [SignRules] 的后置规则。
 *
 * 这样 [sign] 只剩下「一条不可拆的结算流水」，新增玩法（节日翻倍、公会加成、签到成就）
 * 都不需要再改这个文件。
 */
object SignUsecase {

    // ==================================================================================
    // 动作一：签到（core.sign）
    // ==================================================================================

    /**
     * 签到主流程。
     *
     * 入口经 KSP 生成的门面 `SignUsecaseActions.sign(event)` 调用，由 `core.sign` 规则链驱动：
     * 走到这里说明前置规则已放行（今天还没签到），且奖励已经算好放在上下文里。
     *
     * 本方法只做「不可拆、不可跳过」的结算动作，每一步都按顺序标注，方便对照排查。
     *
     * @param event 群消息事件（规则链要求第一个参数是事件）。
     * @param context 本次规则链调用的上下文：读前置规则准备的 [SignActor] 与 [SignReward]。
     * @return 成功时返回 [SignOutcome] 供后置规则读取；提前结束（已签到/入账失败）返回 `null`。
     */
    @Action("core.sign")
    suspend fun sign(event: GroupMessageEvent, context: ActionContext): SignOutcome? {
        // 事件参数是规则链的入口契约（第一个参数必须是触发事件），这里只用它打一行来源日志；
        // 业务上需要的用户/群/消息都从下面的 SignActor 快照取，避免同一个事件被反复拆字段。
        Log.info("签到指令:群(${event.group.id}) 用户(${event.sender.id})")

        // ---------- 步骤 1：取本次签到的参与者快照 ----------
        // Provider 在 @Before 阶段已经加载过，这里直接命中同一次调用的缓存，不会重复查库；
        // 关键是拿到的是**同一个 UserInfoDto 实例**，前置规则里补签卡改过的连签数在这里可见。
        val actor = context.provide(SignActor::class, SignProvider.SIGNER)
        val userInfo: UserInfoDto = actor.userInfo
        val subject: Contact = actor.subject

        // 引用回复：让「签到成功」这条消息能看出是在回哪句指令。
        // quoteReply 返回的是可变的 MessageChainBuilder，后续一边拼文案一边 build()。
        val messages = MessageUtil.quoteReply(actor.message)

        // ---------- 步骤 2：推进签到状态 ----------
        // 「能不能签」已由 @Before(10) 判定过；这里才真正改 signTime / signNumber / oldSignNumber。
        // 再判一次是防御：判定与推进之间存在时间窗口（例如跨日重置点正好落在两次调用之间）。
        if (!SignService.applySign(userInfo, EconomyRuntime.config.reSignTime)) {
            subject.sendMessage("你已经签到过了哦!")
            return null
        }

        // ---------- 步骤 3：取出前置规则算好的奖励 ----------
        // 奖励（基础金币 + 称号/道具倍率 + 待消耗道具）由 @Before(20) 产出入上下文。
        val reward = context.get(SignReward::class)
        if (reward == null) {
            // 正常情况下不可能发生（前置规则一定会 put）；真发生了说明规则被改动过，记录后放弃结算。
            Log.warning("签到:前置规则未产出奖励，本次签到终止（用户 ${userInfo.qq}）")
            return null
        }
        val goldNumber: Double = reward.finalReward

        // ---------- 步骤 4：把「触发事件」整理成图片右下角的文本行 ----------
        // 附加文案原本会单独发一条文字消息；现在它是签到图信息区的一部分。
        // 只保留真正触发的事件（至少两行：标题 + 内容），避免没有事件时把空标题画进图片。
        val eventReplyLines = reward.eventReply
            ?.build()
            ?.let(::extractPlainTextLines)
            ?.takeIf { it.size > 1 }
            .orEmpty()

        // ---------- 步骤 5：金币入账 ----------
        // 走新经济接口（BigDecimal + LedgerOp），具体落到 economy-base 还是上游 mirai-economy
        // 由 Config.yml 的 economyBackend 决定，本方法不感知。
        // 账户标识用 userInfo.qq 而不是 userInfo.user：前者不依赖 mirai User 对象，
        // 与账本主键 u<qq> 一一对应，也让这段逻辑可以在没有 mirai 运行时的测试里跑。
        val wallet = UserWallet()
        val rewardAmount = UserWallet.money(goldNumber)
        val credit = wallet.credit(userInfo.qq, rewardAmount, userInfo.name)
        if (!credit.success) {
            Log.warning(
                "签到:金币入账失败（用户 ${userInfo.qq}，金额 ${rewardAmount.toPlainString()}，" +
                    "后端 ${wallet.backendId}，原因 ${credit.message}）"
            )
            subject.sendMessage("签到失败!")
            return null
        }

        // ---------- 步骤 6：扣掉本次消耗的道具，并把用户信息落库 ----------
        // 待消耗道具由 signProp 登记（月卡过期的、用掉的双倍/三倍/补签卡）。
        userInfo.signEarnings = goldNumber
        reward.propsToConsume.forEach { backpack ->
            BackpackManager.delPropToBackpack(userInfo, backpack)
        }
        UserManager.saveUserInfo(userInfo)

        // ---------- 步骤 7：组装回复文案 ----------
        // 同一份文案有两个去处：消息链（图片发不出去时降级用）与图片信息区（逐行绘制）。
        // 余额优先用入账返回的快照（就是入账后的真实值），拿不到才回查一次账本。
        val moneyByUser: BigDecimal = credit.balance ?: wallet.balance(userInfo.qq)
        val signInfoLines = mutableListOf<String>()

        messages.append(PlainText("签到成功!\n"))
        signInfoLines += "签到成功!"

        val moneyLine =
            "金币:${MoneyFormatUtil.format(moneyByUser.toDouble())}(+${MoneyFormatUtil.format(goldNumber)})"
        messages.append(PlainText("$moneyLine\n"))
        signInfoLines += moneyLine

        // 档位运气文案（如「哇偶,你今天运气爆棚…」）。
        val reply = reward.reply
        if (reply != null) {
            messages.add(reply)
            signInfoLines += extractPlainTextLines(reply)
        }

        // 断签提示：本次是断签后重签，提示上一条连签断在哪。
        if (userInfo.oldSignNumber != 0) {
            val oldSignLine = "你的连签线断在了${userInfo.oldSignNumber}天,可惜~"
            messages.append(oldSignLine)
            signInfoLines += oldSignLine
        }

        // 称号 / 月卡 / 各类卡片产生的附加事件行。
        signInfoLines += eventReplyLines

        // ---------- 步骤 8：渲染并发送签到图（失败降级为文本） ----------
        sendSignImage(userInfo, subject, messages.build(), signInfoLines.joinToString("\n"))

        // ---------- 步骤 9：交出结果快照 ----------
        // 返回值由引擎写入上下文，@After 规则用 actionResult(SignOutcome::class) 读取；
        // 到这行为止才算「真的签成了」，后置日志与称号检查都以它为准。
        return SignOutcome(
            qq = userInfo.qq,
            gold = rewardAmount,
            signNumber = userInfo.signNumber,
            groupId = actor.groupId,
        )
    }

    // ==================================================================================
    // 动作二：关闭本群签到（core.sign.close）
    // ==================================================================================

    /**
     * 关闭本群签到。
     *
     * 实现方式是把**群**加入签到黑名单权限组：签到指令上挂的是
     * `blackPermissions = [EconPerm.SIGN_BLACK_PERM]`，所以进了黑名单组就发不出签到。
     *
     * 指令层已经用 `@MessageAuthorize(userPermissions = [OWNER, ADMIN])` 限定了管理员，
     * 这里不再重复做权限判断，只处理业务状态。
     *
     * @param event 群消息事件（第一个参数固定是事件，动作才能被门面调用）。
     */
    @Action("core.sign.close")
    suspend fun closeSign(event: GroupMessageEvent) {
        val group: Group = event.group
        val util = PermUtil
        val user = UserUtil.group(group.id)

        // 步骤 1：已经在黑名单里 → 直接提示，不再重复加入。
        if (util.checkUserHasPerm(user, EconPerm.SIGN_BLACK_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的签到已经关闭了!"))
            return
        }

        // 步骤 2：加入签到黑名单组，按返回结果回复成功/失败。
        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.SIGN_BLACK_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的签到关闭成功!"))
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的签到关闭失败!"))
        }
    }

    // ==================================================================================
    // 动作三：开启本群签到（core.sign.open）
    // ==================================================================================

    /**
     * 重新开启本群签到（把群从签到黑名单组里移除）。
     *
     * @param event 群消息事件。
     */
    @Action("core.sign.open")
    suspend fun openSign(event: GroupMessageEvent) {
        val group: Group = event.group
        val util = PermUtil
        val user = UserUtil.group(group.id)

        // 步骤 1：本来就不在黑名单里 → 说明签到一直是开着的。
        if (!util.checkUserHasPerm(user, EconPerm.SIGN_BLACK_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的签到已经开启!"))
            return
        }

        // 步骤 2：从黑名单组里摘掉这个群，并立即保存权限组。
        val permGroup: PermGroup = util.takePermGroupByName(EconPerm.GROUP.SIGN_BLACK_GROUP)
        permGroup.users.remove(user)
        permGroup.save()

        group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的签到开启成功!"))
    }

    // ==================================================================================
    // 动作四：刷新签到状态（core.sign.refresh）
    // ==================================================================================

    /**
     * 刷新签到状态：把用户的 `signTime` 往前拨一天，使其立刻可以再签一次。
     *
     * 这是管理员纠错指令（例如签到卡在跨日边界上），属于**有意破坏连签口径**的操作，
     * 所以后置规则里会留一条审计日志。
     *
     * @param event 群消息事件。
     */
    @Action("core.sign.refresh")
    suspend fun refreshSign(event: GroupMessageEvent) {
        val group: Group = event.group
        val sender: Member = event.sender

        // 步骤 1：取用户信息，把上次签到时间往前挪一天（= 重新满足「已过下一签到点」）。
        val userInfo: UserInfoDto = UserManager.getUserInfo(sender)
        val dateTime: DateTime = DateUtil.offsetDay(Date(userInfo.signTime), -1)
        userInfo.signTime = dateTime.time

        // 步骤 2：落库后回复结果。
        UserManager.saveUserInfo(userInfo)
        group.sendMessage(MessageUtil.formatMessageChain(event.message, "签到刷新成功!"))
    }

    // ==================================================================================
    // 图片渲染（私有，动作内部的实现细节，不参与规则链）
    // ==================================================================================

    /**
     * 发送签到结果图；生成或发送失败时退回纯文本，保证玩家一定能看到结算结果。
     *
     * @param userInfo 已结算完成的用户信息（含最新金币、连签）。
     * @param subject 发送目标（群）。
     * @param messages 完整消息链（降级时直接发它，保留引用回复与运气文案）。
     * @param signInfoText 图片右下角信息区文本（按行拼接）。
     */
    private suspend fun sendSignImage(
        userInfo: UserInfoDto,
        subject: Contact,
        messages: MessageChain,
        signInfoText: String,
    ) {
        // 签到图复用个人信息底图，但右下角信息区改成显示本次签到详情。
        // 这样「签到成功 / 金币 / 随机事件 / 道具事件」都由统一卡片绘制流程负责排版。
        val userInfoImageBase: BufferedImage = runCatching {
            withContext(EconomyAsyncService.coroutineDispatcher()) {
                val card = UserManager.buildUserInfoCard(
                    userInfo = userInfo,
                    infoTitle = "签到信息",
                    infoText = signInfoText,
                    infoSignature = "",
                )
                UserManager.renderUserInfoCard(userInfo, card)
            }
        }.getOrElse { e ->
            Log.error("签到管理:签到图片生成错误!", e)
            null
        } ?: run {
            // 画图失败：直接退回文本，别让玩家以为签到没生效。
            sendSignFallback(subject, messages, signInfoText)
            return
        }

        try {
            ImageMessageUtil.sendImage(subject, userInfoImageBase)
        } catch (e: Exception) {
            Log.error("签到管理:签到图片发送错误!", e)
            sendSignFallback(subject, messages, signInfoText)
        }
    }

    /**
     * 图片路径失败时的降级出口：优先发同一份面板文本，保证触发事件不会因回退而丢失。
     *
     * @param subject 发送目标。
     * @param messages 完整消息链。
     * @param signInfoText 面板文本。
     */
    private suspend fun sendSignFallback(subject: Contact, messages: MessageChain, signInfoText: String) {
        if (signInfoText.isBlank()) {
            subject.sendMessage(messages)
        } else {
            subject.sendMessage(MessageUtil.formatMessageChain(signInfoText))
        }
    }

    /**
     * 从消息链里抽出纯文本行。
     *
     * 消息链可能带 QuoteReply、图片或其他类型，而图片右下角面板只需要纯文本；
     * 这里按换行拆开、去空行，便于渲染器逐行折行，也避免把「被回复的原指令」画进图片。
     *
     * @param messages 待抽取的消息链。
     * @return 非空的纯文本行列表。
     */
    private fun extractPlainTextLines(messages: MessageChain): List<String> {
        return messages
            .filterIsInstance<PlainText>()
            .flatMap { it.content.split('\n') }
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
