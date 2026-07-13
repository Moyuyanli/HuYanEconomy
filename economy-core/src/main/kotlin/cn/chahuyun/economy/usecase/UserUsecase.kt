package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.manager.UserStatusManager
import cn.chahuyun.economy.model.props.PropsCard
import cn.chahuyun.economy.model.user.getString
import cn.chahuyun.economy.model.yiyan.YiYan
import cn.chahuyun.economy.plugin.YiYanManager
import cn.chahuyun.economy.utils.*
import net.mamoe.mirai.contact.AvatarSpec
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply

object UserUsecase {

    suspend fun getUserInfoImage(event: MessageEvent) {
        Log.info("个人信息指令")

        val subject = event.subject
        val sender = event.sender

        val userInfo = UserCoreManager.getUserInfo(sender)
        TitleManager.checkMonopolyJava(userInfo, subject)

        val textFallback = buildUserInfoText(event)

        val yiYan: YiYan = YiYanManager.getYiyan()

        val hitokoto = yiYan.hitokoto ?: "今日也要好好经营。"
        val signature = "--" + (yiYan.author ?: "无名") + ":" + (yiYan.from ?: "未知")
        val userInfoImageBase = runCatching {
            val card = UserCoreManager.buildUserInfoCard(userInfo, hitokoto, signature)
            UserCoreManager.renderUserInfoCard(userInfo, card)
        }.getOrElse { e ->
            Log.error("用户管理:个人信息图片生成错误", e)
            null
        }
        if (userInfoImageBase == null) {
            subject.sendMessage(textFallback)
            return
        }

        try {
            ImageMessageUtil.sendImage(subject, userInfoImageBase)
        } catch (e: Exception) {
            Log.error("用户管理:个人信息图片发送错误", e)
            subject.sendMessage(textFallback)
            return
        }
    }

    suspend fun getUserInfoText(event: MessageEvent) {
        Log.info("个人信息文字指令")
        event.subject.sendMessage(buildUserInfoText(event))
    }

    private suspend fun buildUserInfoText(event: MessageEvent) = MessageChainBuilder().apply {
        val subject = event.subject
        val sender = event.sender
        val userInfo = UserCoreManager.getUserInfo(sender)
        TitleManager.checkMonopolyJava(userInfo, subject)

        try {
            append(ImageMessageUtil.uploadImageFromUrl(subject, sender.avatarUrl(AvatarSpec.LARGE)))
        } catch (e: Exception) {
            Log.error("用户管理:查询个人信息上传头像出错!", e)
        }

        append(userInfo.getString())
        append("金币:${MoneyFormatUtil.format(EconomyUtil.getMoneyByUser(sender))}")
    }.build()

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

    private fun moneyRatio(amount: Double, total: Double): String =
        if (total <= 0.0) "0.0%" else "${FormatUtil.fixed(amount / total * 100.0, 1)}%"

    suspend fun discharge(event: GroupMessageEvent) {
        val user: Member = event.sender
        val userInfo = UserCoreManager.getUserInfo(user)

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(event.message))

        val group: Group = event.subject
        if (UserStatusManager.checkUserInHospital(userInfo)) {
            val userStatus = UserStatusManager.getUserStatus(userInfo)

            val price = userStatus.recoveryTime.toDouble() * 3.0
            val real: Double

            if (BackpackManager.checkPropInUser(userInfo, PropsCard.HEALTH)) {
                real = cn.chahuyun.economy.utils.ShareUtils.rounding(price * 0.8)
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
            return
        }
        group.sendMessage(MessageUtil.formatMessageChain(event.message, "你不在医院，你出什么院？"))
    }
}
