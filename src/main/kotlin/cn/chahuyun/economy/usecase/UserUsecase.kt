package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.manager.UserStatusManager
import cn.chahuyun.economy.model.props.PropsCard
import cn.chahuyun.economy.model.yiyan.YiYan
import cn.chahuyun.economy.plugin.YiYanManager
import cn.chahuyun.economy.utils.*
import net.mamoe.mirai.contact.AvatarSpec
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO

object UserUsecase {

    suspend fun getUserInfoImage(event: MessageEvent) {
        Log.info("个人信息指令")

        val subject = event.subject
        val sender = event.sender

        val userInfo = UserCoreManager.getUserInfo(sender)
        TitleManager.checkMonopolyJava(userInfo, subject)

        val moneyByUser = EconomyUtil.getMoneyByUser(sender)
        val singleMessages = MessageChainBuilder()

        try {
            URL(sender.avatarUrl(AvatarSpec.LARGE)).openConnection().getInputStream().use { input ->
                input.toExternalResource().use { resource ->
                    val image: Image = subject.uploadImage(resource)
                    singleMessages.append(image)
                }
            }
        } catch (e: Exception) {
            Log.error("用户管理:查询个人信息上传头像出错!", e)
        }

        singleMessages.append(userInfo.getString()).append("金币:${MoneyFormatUtil.format(moneyByUser)}")

        val yiYan: YiYan = YiYanManager.getYiyan()

        val hitokoto = yiYan.hitokoto ?: "今日也要好好经营。"
        val signature = "--" + (yiYan.author ?: "无名") + ":" + (yiYan.from ?: "未知")
        val userInfoImageBase = UserCoreManager.getUserInfoImageBase(userInfo, hitokoto, signature)
        if (userInfoImageBase == null) {
            subject.sendMessage(singleMessages.build())
            return
        }

        val stream = ByteArrayOutputStream()
        try {
            ImageIO.write(userInfoImageBase, "png", stream)
        } catch (e: Exception) {
            Log.error("用户管理:个人信息图片发送错误", e)
            subject.sendMessage(singleMessages.build())
            return
        }

        ByteArrayInputStream(stream.toByteArray()).toExternalResource().use { resource ->
            val image = subject.uploadImage(resource)
            subject.sendMessage(image)
        }
    }

    suspend fun moneyInfo(event: MessageEvent) {
        val subject = event.subject
        val message = event.message
        val user = event.sender

        val money = EconomyUtil.getMoneyByUser(user)
        val bank = EconomyUtil.getMoneyByBank(user)

        subject.sendMessage(
            MessageUtil.formatMessageChain(
                message,
                "你的经济状况:\n" +
                    "钱包余额:${FormatUtil.fixed(money, 1)}\n" +
                    "银行存款:${FormatUtil.fixed(bank, 1)}"
            )
        )
    }

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
