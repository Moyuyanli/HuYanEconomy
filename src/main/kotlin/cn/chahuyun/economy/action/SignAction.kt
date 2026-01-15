package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.constant.ImageDrawXY
import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.props.PropsCard
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.plugin.ImageManager
import cn.chahuyun.economy.plugin.PluginManager
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.sign.SignEvent
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.ImageUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateTime
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO

/**
 * 签到管理
 */
@EventComponent
class SignAction {

    /**
     * 签到
     */
    @MessageAuthorize(text = ["签到", "打卡", "sign"], blackPermissions = [EconPerm.SIGN_BLACK_PERM])
    suspend fun sign(event: GroupMessageEvent) {
        Log.info("签到指令")

        val user: User = event.sender
        val subject: Contact = event.subject
        val message: MessageChain = event.message

        var userInfo: UserInfo = UserCoreManager.getUserInfo(user)

        val messages = MessageUtil.quoteReply(message)

        if (!userInfo.sign()) {
            messages.append(PlainText("你已经签到过了哦!"))
            subject.sendMessage(messages.build())
            return
        }

        val signEvent = SignEvent(userInfo, event)
        signEvent.param = RandomUtil.randomInt(0, 1001)
        signEvent.eventReplyAdd(MessageUtil.formatMessageChain(message, "本次签到触发事件:"))

        val broadcast = signEvent.broadcast()

        val goldNumber = broadcast.gold ?: 0.0
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
        HibernateFactory.merge(userInfo)

        val moneyByUser = EconomyUtil.getMoneyByUser(userInfo.user)
        messages.append(PlainText("签到成功!\n"))
        messages.append(PlainText(String.format("金币:%s(+%s)\n", moneyByUser, goldNumber)))
        if (reply != null) {
            messages.add(reply as net.mamoe.mirai.message.data.Message)
        }
        if (userInfo.oldSignNumber != 0) {
            messages.append(String.format("你的连签线断在了%d天,可惜~", userInfo.oldSignNumber))
        }

        TitleManager.checkSignTitleJava(userInfo, subject)
        TitleManager.checkMonopolyJava(userInfo, subject)

        sendSignImage(userInfo, subject, messages.build())
    }

    companion object {
        private var index = 0

        /**
         * 签到金钱获取
         * 优先级: EventPriority.HIGH
         */
        @JvmStatic
        fun randomSignGold(event: SignEvent) {
            var goldNumber: Double
            val builder = MessageChainBuilder()

            val param = event.param ?: 0

            if (param <= 500) {
                goldNumber = RandomUtil.randomInt(50, 101).toDouble()
            } else if (param <= 850) {
                goldNumber = RandomUtil.randomInt(100, 201).toDouble()
                builder.add(PlainText(String.format("哇偶,你今天运气爆棚,获得%s金币", goldNumber)))
            } else if (param <= 999) {
                goldNumber = RandomUtil.randomInt(200, 501).toDouble()
                builder.add(PlainText(String.format("卧槽,你家祖坟裂了,冒出%s金币", goldNumber)))
            } else {
                goldNumber = 999.0
                builder.add(PlainText("你™直接天降神韵!"))
            }

            event.gold = goldNumber
            event.reply = builder.build()
        }

        /**
         * 自定义签到事件
         * 优先级: EventPriority.NORMAL
         */
        @JvmStatic
        fun signProp(event: SignEvent) {
            val userInfo = event.userInfo
            var multiples = 1

            if (TitleManager.checkTitleIsOnEnable(userInfo, TitleCode.SIGN_15)) {
                multiples += 1
                event.eventReplyAdd(MessageUtil.formatMessageChain("装备签到狂人称号，本次签到奖励翻倍!"))
            } else if (TitleManager.checkTitleIsOnEnable(userInfo, TitleCode.SIGN_90)) {
                multiples += 4
                event.eventReplyAdd(MessageUtil.formatMessageChain("装备签到大王称号，本次签到奖励翻5倍!"))
            }

            val backpacks = userInfo.backpacks
            val list = backpacks.toList()

            if (BackpackManager.checkPropInUser(userInfo, PropsCard.MONTHLY)) {
                val prop = userInfo.getProp(PropsCard.MONTHLY)
                if (PropsManager.getProp(prop, PropsCard::class.java).status) {
                    event.sign_2 = true
                    event.sign_3 = true
                    multiples += 4
                    event.eventReplyAdd(MessageUtil.formatMessageChain("已启用签到月卡,本次签到奖励翻5倍!"))
                }
            }

            for (backpack in list) {
                val propId = backpack.propId
                val propCode = backpack.propCode
                if (propId == null || propCode == null) continue
                when (propCode) {
                    PropsCard.SIGN_2 -> {
                        val card = try {
                            PropsManager.deserialization(propId, PropsCard::class.java)
                        } catch (e: Exception) {
                            BackpackManager.delPropToBackpack(userInfo, propId)
                            continue
                        }
                        if (event.sign_2) {
                            continue
                        }
                        if (card.status) {
                            multiples += 1
                            BackpackManager.delPropToBackpack(userInfo, propId)
                            event.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张双倍签到卡，本次签到奖励翻倍!"))
                            event.sign_2 = true
                        }
                    }

                    PropsCard.SIGN_3 -> {
                        val card = try {
                            PropsManager.deserialization(propId, PropsCard::class.java)
                        } catch (e: Exception) {
                            BackpackManager.delPropToBackpack(userInfo, propId)
                            continue
                        }
                        if (event.sign_3) {
                            continue
                        }
                        if (card.status) {
                            multiples += 2
                            BackpackManager.delPropToBackpack(userInfo, propId)
                            event.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张三倍签到卡，本次签到奖励三翻倍!"))
                            event.sign_3 = true
                        }
                    }

                    PropsCard.SIGN_IN -> {
                        try {
                            PropsManager.deserialization(propId, PropsCard::class.java)
                        } catch (e: Exception) {
                            BackpackManager.delPropToBackpack(userInfo, propId)
                            continue
                        }
                        if (event.sign_in) {
                            continue
                        }

                        val oldSignNumber = userInfo.oldSignNumber
                        if (oldSignNumber == 0) {
                            break
                        }

                        userInfo.signNumber = userInfo.signNumber + oldSignNumber
                        userInfo.oldSignNumber = 0

                        val useEvent = UseEvent(userInfo.user, event.group, userInfo)
                        PropsManager.usePropJava(backpack, useEvent)
                        event.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张补签卡，续上断掉的签到天数!"))
                        event.sign_in = true
                    }
                }
            }

            event.gold = (event.gold ?: 0.0) * multiples
        }

        private suspend fun sendSignImage(userInfo: UserInfo, subject: Contact, messages: MessageChain) {
            val userInfoImageBase: BufferedImage = UserCoreManager.getUserInfoImageBase(userInfo) ?: run {
                subject.sendMessage(messages)
                return
            }
            val graphics: Graphics2D = ImageUtil.getG2d(userInfoImageBase)
            if (PluginManager.isCustomImage) {
                graphics.color = Color.BLACK
                graphics.font = ImageManager.getCustomFont()
                ImageUtil.drawString(messages.contentToString(), ImageDrawXY.A_WORD.x, ImageDrawXY.A_WORD.y, 440, graphics)
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

        /**
         * 发送签到图片
         */
        @Deprecated("已废弃")
        @JvmStatic
        suspend fun sendSignImage(
            userInfo: UserInfo,
            user: User,
            subject: Contact,
            money: Double,
            obtain: Double,
            messages: MessageChain
        ) {
            val instance = HuYanEconomy
            try {
                val indexSuffix = if (index % 4 == 0) 4 else index % 4
                val asStream: InputStream = instance.getResourceAsStream("sign$indexSuffix.png") ?: return
                index++
                val image = ImageIO.read(asStream)
                val pen = ImageUtil.getG2d(image)

                val avatarUrl = user.avatarUrl(AvatarSpec.LARGE)
                ImageIO.read(URL(avatarUrl))

                pen.color = Color.WHITE
                pen.font = Font("黑体", Font.BOLD, 60)
                pen.drawString(userInfo.name, 200, 155)
                if (money.toString().length > 5) {
                    pen.font = Font("黑体", Font.PLAIN, 24)
                    pen.color = Color.black
                    pen.drawString(money.toString(), 600, 410)
                    pen.font = Font("黑体", Font.PLAIN, 28)
                } else {
                    pen.font = Font("黑体", Font.PLAIN, 28)
                    pen.color = Color.black
                    pen.drawString(money.toString(), 600, 410)
                }
                pen.drawString(userInfo.qq.toString(), 172, 240)
                pen.drawString(obtain.toString(), 810, 410)
                pen.font = Font("黑体", Font.PLAIN, 23)
                pen.drawString(DateUtil.format(userInfo.signTime, "yyyy-MM-dd HH:mm:ss"), 172, 320)
                pen.drawString(userInfo.signNumber.toString(), 172, 360)
                pen.drawString(DateUtil.format(DateUtil.offsetDay(userInfo.signTime, 1), "yyyy-MM-dd HH:mm:ss"), 221, 402)
                pen.drawString("暂无", 172, 440)
                val x = AtomicInteger(210)
                pen.font = Font("黑体", Font.PLAIN, 22)
                messages.forEach { v ->
                    pen.drawString(v.contentToString(), 520, x.get())
                    x.addAndGet(28)
                }

                val stream = ByteArrayOutputStream()
                ImageIO.write(image, "png", stream)
                ByteArrayInputStream(stream.toByteArray()).toExternalResource().use { resource ->
                    val imageMessage = subject.uploadImage(resource)
                    subject.sendMessage(imageMessage)
                }
            } catch (e: IOException) {
                Log.error(e)
            }
        }

        /**
         * 将签到图片删除并重新复制
         */
        private fun refreshSignImage() {
            val instance = HuYanEconomy
            val indexSuffix = if (index % 4 == 0) 4 else index % 4
            val asStream: InputStream = instance.getResourceAsStream("sign$indexSuffix.png") ?: return
            index++
            val file: File = instance.resolveDataFile("sign.png")
            if (file.exists()) {
                if (file.delete()) {
                    Log.debug("签到管理:签到图片刷新成功")
                }
            }
            try {
                Files.copy(asStream, file.toPath())
            } catch (e: IOException) {
                Log.error("签到管理:签到图片刷新失败", e)
            }
        }
    }

    @MessageAuthorize(text = ["关闭 签到"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
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

    @MessageAuthorize(text = ["开启 签到"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
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

    @MessageAuthorize(text = ["刷新签到"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun refreshSign(event: GroupMessageEvent) {
        val group: Group = event.group
        val sender: Member = event.sender

        val userInfo: UserInfo = UserCoreManager.getUserInfo(sender)
        val dateTime: DateTime = DateUtil.offsetDay(userInfo.signTime, -1)
        userInfo.signTime = dateTime

        HibernateFactory.merge(userInfo)
        group.sendMessage(MessageUtil.formatMessageChain(event.message, "签到刷新成功!"))
    }
}
