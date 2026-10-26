package cn.chahuyun.economy.feature.user

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.model.user.getString
import cn.chahuyun.economy.model.yiyan.YiYan
import cn.chahuyun.economy.plugin.YiYanManager
import cn.chahuyun.economy.service.EconomyAsyncService
import cn.chahuyun.economy.utils.*
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.AvatarSpec
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder

/**
 * 用户功能的主用例：个人信息（图/文）与资产总览。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `core.user.info` | 个人信息 / info | `@Before(10)` 大富翁称号检查；`@After(10)` 查看日志 |
 * | `core.user.infoText` | info# / 我的信息# … | 同上（文字版同样先修正称号） |
 * | `core.user.money` | money / 经济信息 / 我的资金 | 无（纯查询） |
 *
 * 「大富翁」称号检查放在 `@Before` 而不是动作里：个人信息卡要渲染当前启用称号，
 * 必须**先修正称号再画图**，否则玩家本次看到的是旧称号（签到那边的称号检查在 `@After`，
 * 因为签到图不依赖称号渲染）。
 *
 * 「出院」已按归属迁到 `feature/status`（`core.status.discharge`），本文件不再处理位置状态。
 */
object UserUsecase {

    /**
     * 查看个人信息卡片（`core.user.info`），图片生成/发送失败时自动降级为文字。
     *
     * @param event 消息事件（群聊或好友）。
     * @param context 本次调用上下文，读取 [UserActor]。
     */
    @Action("core.user.info")
    suspend fun getUserInfoImage(event: MessageEvent, context: ActionContext) {
        Log.info("个人信息指令:用户(${event.sender.id})")

        val actor = context.provide(UserActor::class, UserProvider.ACTOR)
        val subject = actor.subject
        val userInfo = actor.userInfo

        val textFallback = buildUserInfoText(actor)

        val yiYan: YiYan = YiYanManager.getYiyan()

        val hitokoto = yiYan.hitokoto ?: "今日也要好好经营。"
        val signature = "--" + (yiYan.author ?: "无名") + ":" + (yiYan.from ?: "未知")
        val userInfoImageBytes = runCatching {
            withContext(EconomyAsyncService.coroutineDispatcher()) {
                val card = UserManager.buildUserInfoCard(userInfo, hitokoto, signature)
                val image = UserManager.renderUserInfoCard(userInfo, card)
                image?.let { ImageMessageUtil.toSizeLimitedImageBytes(it) }
            }
        }.getOrElse { e ->
            Log.error("用户管理:个人信息图片生成错误", e)
            null
        }
        if (userInfoImageBytes == null) {
            subject.sendMessage(textFallback)
            return
        }

        try {
            ImageMessageUtil.sendImageBytes(subject, userInfoImageBytes)
        } catch (e: Exception) {
            Log.error("用户管理:个人信息图片发送错误", e)
            subject.sendMessage(textFallback)
        }
    }

    /**
     * 查看个人信息文字版（`core.user.infoText`）。
     *
     * @param event 消息事件。
     * @param context 本次调用上下文，读取 [UserActor]。
     */
    @Action("core.user.infoText")
    suspend fun getUserInfoText(event: MessageEvent, context: ActionContext) {
        Log.info("个人信息文字指令:用户(${event.sender.id})")
        val actor = context.provide(UserActor::class, UserProvider.ACTOR)
        actor.subject.sendMessage(buildUserInfoText(actor))
    }

    /**
     * 资产总览（`core.user.money`）：钱包、主银行、各私人银行存款与占比。
     *
     * @param event 消息事件。
     */
    @Action("core.user.money")
    suspend fun moneyInfo(event: MessageEvent) {
        val subject = event.subject
        val message = event.message
        val user = event.sender

        val money = EconomyUtil.getMoneyByUser(user)
        val bank = EconomyUtil.getMoneyByBank(user)
        val privateDeposits = PrivateBankRepository.listBanks()
            .mapNotNull { privateBank ->
                val deposit = PrivateBankRepository.findDeposit(privateBank.code, user.id) ?: return@mapNotNull null
                if (deposit.principal <= 0.0) return@mapNotNull null
                privateBank.name.ifBlank { privateBank.code } to deposit.principal
            }
            .sortedByDescending { it.second }
        val privateTotal = privateDeposits.sumOf { it.second }
        val total = money + bank + privateTotal
        val privateDepositText = if (privateDeposits.isEmpty()) {
            "私人银行存款:0 (${moneyRatio(0.0, total)})"
        } else {
            buildString {
                append("私人银行存款合计:")
                    .append(MoneyFormatUtil.format(privateTotal))
                    .append(" (").append(moneyRatio(privateTotal, total)).append(")")
                    .append('\n')
                privateDeposits.forEach { (bankName, amount) ->
                    append("- ").append(bankName)
                        .append(":").append(MoneyFormatUtil.format(amount))
                        .append(" (").append(moneyRatio(amount, total)).append(")")
                        .append('\n')
                }
            }.trimEnd()
        }

        subject.sendMessage(
            MessageUtil.formatMessageChain(
                message,
                "你的经济状况:\n" +
                    "钱包余额:${MoneyFormatUtil.format(money)} (${moneyRatio(money, total)})\n" +
                    "主银行存款:${MoneyFormatUtil.format(bank)} (${moneyRatio(bank, total)})\n" +
                    "$privateDepositText\n" +
                    "总资产:${MoneyFormatUtil.format(total)}"
            )
        )
    }

    private suspend fun buildUserInfoText(actor: UserActor) = MessageChainBuilder().apply {
        val subject = actor.subject
        val sender = actor.member
        val userInfo = actor.userInfo

        try {
            append(ImageMessageUtil.uploadImageFromUrl(subject, sender.avatarUrl(AvatarSpec.LARGE)))
        } catch (e: Exception) {
            Log.error("用户管理:查询个人信息上传头像出错!", e)
        }

        append(userInfo.getString())
        append("金币:${MoneyFormatUtil.format(EconomyUtil.getMoneyByUser(sender))}")
    }.build()

    private fun moneyRatio(amount: Double, total: Double): String =
        if (total <= 0.0) "0.0%" else "${FormatUtil.fixed(amount / total * 100.0, 1)}%"
}
