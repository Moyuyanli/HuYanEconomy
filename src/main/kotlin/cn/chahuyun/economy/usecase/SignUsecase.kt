package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.sign.BeforeSignEvent
import cn.chahuyun.economy.sign.SignCommittedEvent
import cn.chahuyun.economy.sign.SignRewardEvent
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateTime
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

object SignUsecase {

    suspend fun sign(event: GroupMessageEvent) {
        Log.info("签到指令")

        val user: User = event.sender
        val subject: Contact = event.subject
        val message: MessageChain = event.message

        var userInfo: UserInfoDto = UserCoreManager.getUserInfo(user)

        val messages = MessageUtil.quoteReply(message)

        val beforeEvent = BeforeSignEvent(userInfo, event).broadcast()
        if (beforeEvent.cancelled) {
            messages.append(beforeEvent.cancelMessage ?: PlainText("签到已取消!"))
            subject.sendMessage(messages.build())
            return
        }

        if (!userInfo.sign()) {
            messages.append(PlainText("你已经签到过了哦!"))
            subject.sendMessage(messages.build())
            return
        }

        val signEvent = SignRewardEvent(userInfo, event)
        signEvent.param = RandomUtil.randomInt(0, 1001)
        signEvent.eventReplyAdd(MessageUtil.formatMessageChain(message, "本次签到触发事件:"))

        val broadcast = signEvent.broadcast()

        val goldNumber = broadcast.finalReward
        val reply = broadcast.reply
        userInfo = broadcast.userInfo

        // 触发事件原本会单独发送一条文字消息；现在它是签到图右下角信息区的一部分。
        // 只保留真正触发的事件，避免没有事件时把“本次签到触发事件:”空标题画进图片。
        val eventReplyLines = signEvent.eventReply
            ?.build()
            ?.let(::extractPlainTextLines)
            ?.takeIf { it.size > 1 }
            .orEmpty()

        if (!EconomyUtil.plusMoneyToUser(userInfo.user, goldNumber)) {
            subject.sendMessage("签到失败!")
            return
        }

        userInfo.signEarnings = goldNumber
        broadcast.propsToConsume.forEach { backpack ->
            BackpackManager.delPropToBackpack(userInfo, backpack)
        }
        UserCoreManager.saveUserInfo(userInfo)

        val moneyByUser = EconomyUtil.getMoneyByUser(userInfo.user)
        val signInfoLines = mutableListOf<String>()

        messages.append(PlainText("签到成功!\n"))
        signInfoLines += "签到成功!"

        val moneyLine = "金币:${MoneyFormatUtil.format(moneyByUser)}(+${MoneyFormatUtil.format(goldNumber)})"
        messages.append(PlainText("$moneyLine\n"))
        signInfoLines += moneyLine

        if (reply != null) {
            messages.add(reply as net.mamoe.mirai.message.data.Message)
            signInfoLines += extractPlainTextLines(reply)
        }
        if (userInfo.oldSignNumber != 0) {
            val oldSignLine = "你的连签线断在了${userInfo.oldSignNumber}天,可惜~"
            messages.append(oldSignLine)
            signInfoLines += oldSignLine
        }
        signInfoLines += eventReplyLines

        TitleManager.checkSignTitleJava(userInfo, subject)
        TitleManager.checkMonopolyJava(userInfo, subject)

        val resultMessages = messages.build()
        SignCommittedEvent(userInfo, event, goldNumber, resultMessages).broadcast()
        sendSignImage(userInfo, subject, resultMessages, signInfoLines.joinToString("\n"))
    }

    suspend fun offSign(event: GroupMessageEvent) {
        val group: Group = event.group

        val util = PermUtil
        val user = UserUtil.group(group.id)

        if (util.checkUserHasPerm(user, EconPerm.SIGN_BLACK_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的签到已经关闭了!"))
            return
        }

        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.SIGN_BLACK_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的签到关闭成功!"))
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的签到关闭失败!"))
        }
    }

    suspend fun startSign(event: GroupMessageEvent) {
        val group: Group = event.group

        val util = PermUtil
        val user = UserUtil.group(group.id)

        if (!util.checkUserHasPerm(user, EconPerm.SIGN_BLACK_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的签到已经开启!"))
            return
        }

        val permGroup: PermGroup = util.takePermGroupByName(EconPerm.GROUP.SIGN_BLACK_GROUP)
        permGroup.users.remove(user)
        permGroup.save()

        group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的签到开启成功!"))
    }

    suspend fun refreshSign(event: GroupMessageEvent) {
        val group: Group = event.group
        val sender: Member = event.sender

        val userInfo: UserInfoDto = UserCoreManager.getUserInfo(sender)
        val dateTime: DateTime = DateUtil.offsetDay(Date(userInfo.signTime), -1)
        userInfo.signTime = dateTime.time

        UserCoreManager.saveUserInfo(userInfo)
        group.sendMessage(MessageUtil.formatMessageChain(event.message, "签到刷新成功!"))
    }

    private suspend fun sendSignImage(
        userInfo: UserInfoDto,
        subject: Contact,
        messages: MessageChain,
        signInfoText: String,
    ) {
        // 签到图复用个人信息底图，但右下角信息区改成显示本次签到详情。
        // 这样“签到成功/金币/随机事件/道具事件”都由统一的卡片绘制流程负责排版。
        val userInfoImageBase: BufferedImage = UserCoreManager.getUserInfoImageBase(
            userInfo = userInfo,
            infoTitle = "签到信息",
            infoText = signInfoText,
            infoSignature = "",
        ) ?: run {
            sendSignFallback(subject, messages, signInfoText)
            return
        }

        val stream = ByteArrayOutputStream()
        try {
            ImageIO.write(userInfoImageBase, "png", stream)
        } catch (e: IOException) {
            Log.error("签到管理:签到图片发送错误!", e)
            sendSignFallback(subject, messages, signInfoText)
            return
        }

        ByteArrayInputStream(stream.toByteArray()).toExternalResource().use { resource ->
            val image = subject.uploadImage(resource)
            subject.sendMessage(image)
        }
    }

    private suspend fun sendSignFallback(subject: Contact, messages: MessageChain, signInfoText: String) {
        // 图片生成失败时也优先发送同一份面板文本，保证触发事件不会因为回退路径丢失。
        if (signInfoText.isBlank()) {
            subject.sendMessage(messages)
        } else {
            subject.sendMessage(MessageUtil.formatMessageChain(signInfoText))
        }
    }

    private fun extractPlainTextLines(messages: MessageChain): List<String> {
        // MessageChain 里可能带 QuoteReply、图片或其他消息类型；右下角面板只需要纯文本。
        // 这里按换行拆开，便于渲染器逐行折行，也避免把被回复的原指令画进图片。
        return messages
            .filterIsInstance<PlainText>()
            .flatMap { it.content.split('\n') }
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
