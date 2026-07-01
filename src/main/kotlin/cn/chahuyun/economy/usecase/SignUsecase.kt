package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.constant.ImageDrawXY
import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.plugin.ImageManager
import cn.chahuyun.economy.plugin.PluginManager
import cn.chahuyun.economy.sign.BeforeSignEvent
import cn.chahuyun.economy.sign.SignCommittedEvent
import cn.chahuyun.economy.sign.SignRewardEvent
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateTime
import cn.hutool.core.date.DateUtil
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
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
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

        val eventReply = signEvent.eventReply
        if (eventReply != null && eventReply.size != 2) {
            subject.sendMessage(eventReply.build())
        }

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
        messages.append(PlainText("签到成功!\n"))
        messages.append(PlainText("金币:${MoneyFormatUtil.format(moneyByUser)}(+${MoneyFormatUtil.format(goldNumber)})\n"))
        if (reply != null) {
            messages.add(reply as net.mamoe.mirai.message.data.Message)
        }
        if (userInfo.oldSignNumber != 0) {
            messages.append("你的连签线断在了${userInfo.oldSignNumber}天,可惜~")
        }

        TitleManager.checkSignTitleJava(userInfo, subject)
        TitleManager.checkMonopolyJava(userInfo, subject)

        val resultMessages = messages.build()
        SignCommittedEvent(userInfo, event, goldNumber, resultMessages).broadcast()
        sendSignImage(userInfo, subject, resultMessages)
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

    private suspend fun sendSignImage(userInfo: UserInfoDto, subject: Contact, messages: MessageChain) {
        val userInfoImageBase: BufferedImage = UserCoreManager.getUserInfoImageBase(userInfo) ?: run {
            subject.sendMessage(messages)
            return
        }
        val graphics: Graphics2D = ImageUtil.getG2d(userInfoImageBase)
        if (PluginManager.isCustomImage) {
            graphics.color = Color.BLACK
            graphics.font = ImageManager.getCustomFont()
            ImageUtil.drawString(
                messages.contentToString(),
                ImageDrawXY.A_WORD.x,
                ImageDrawXY.A_WORD.y,
                440,
                graphics
            )
        } else {
            val fontSize = 20
            graphics.color = Color.black
            val x = AtomicInteger(210)
            graphics.font = Font("黑体", Font.PLAIN, fontSize)
            messages.forEach { v ->
                graphics.drawString(v.contentToString(), 520, x.get())
                x.addAndGet(28)
            }
        }
        graphics.dispose()

        val stream = ByteArrayOutputStream()
        try {
            ImageIO.write(userInfoImageBase, "png", stream)
        } catch (e: IOException) {
            Log.error("签到管理:签到图片发送错误!", e)
            subject.sendMessage(messages)
            return
        }

        ByteArrayInputStream(stream.toByteArray()).toExternalResource().use { resource ->
            val image = subject.uploadImage(resource)
            subject.sendMessage(image)
        }
    }
}
