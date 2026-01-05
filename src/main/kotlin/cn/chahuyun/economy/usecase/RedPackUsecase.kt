package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.entity.redpack.RedPack
import cn.chahuyun.economy.manager.RedPackManager
import cn.chahuyun.economy.repository.RedPackRepository
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.TimeConvertUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import java.util.*

object RedPackUsecase {

    suspend fun create(event: GroupMessageEvent) {
        Log.info("发红包指令")

        val group = event.group
        val sender = event.sender
        val subject = event.subject
        val message = event.message
        val content = message.contentToString()

        val info = content.split(" ").filter { it.isNotBlank() }
        if (info.size < 3) return

        val money = info[1].toDoubleOrNull() ?: return
        val number = info[2].toIntOrNull() ?: return
        var random = false
        if (info.size == 4) {
            random = info[3] == "sj" || info[3] == "随机"
        }

        if (money / number < 0.1) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你发的红包太小了,每个红包金额低于了0.1！"))
            return
        }

        if (money > EconomyUtil.getMoneyByUser(sender)) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你的金币不够啦！"))
            return
        }

        if (!EconomyUtil.plusMoneyToUser(sender, -money)) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "红包发送失败!"))
            return
        }

        val pack = RedPack(
            name = "${sender.nick}的红包",
            groupId = group.id,
            sender = sender.id,
            money = money,
            number = number,
            isRandomPack = random,
            createTime = Date()
        )

        if (random) {
            val doubles = RedPackManager.generateRandomPack(money, number)
            pack.randomPackList = doubles.toMutableList()
        }

        val resultPack = RedPackRepository.saveInTransaction(pack)
        val id = resultPack?.id ?: 0

        if (id == 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "红包创建失败!"))
            return
        }

        if (random) {
            subject.sendMessage(MessageUtil.formatMessageChain(sender.id, "随机红包创建成功!"))
        }

        val prefix = HuYanEconomy.config.prefix
        subject.sendMessage(
            MessageUtil.formatMessageChain(
                "%s 发送了一个红包,快来抢红包吧！%n" +
                        "红包ID:%d%n" +
                        "红包有 %.1f 枚金币%n" +
                        "红包个数%d%n" +
                        "红包发送时间%s%n" +
                        "领取命令 %s领红包 %d",
                sender.nick, id, money, number, TimeConvertUtil.timeConvert(pack.createTime ?: Date()),
                prefix.ifBlank { "" }, id
            )
        )
    }

    suspend fun receive(event: GroupMessageEvent) {
        Log.info("收红包指令")

        val subject = event.subject
        val group = event.group
        val sender = event.sender
        val message = event.message
        val content = message.contentToString()

        val info = content.split(" ").filter { it.isNotBlank() }
        if (info.size < 2) return
        val id = info[1].toIntOrNull() ?: return

        val redPack = RedPackRepository.findById(id)
        if (redPack == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "红包不存在!"))
            return
        }

        if (redPack.groupId != group.id) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "这不是这个群的红包!"))
            return
        }

        if (DateUtil.between(redPack.createTime, Date(), DateUnit.DAY) >= 1) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "这个红包已经过期了"))
            RedPackManager.expireRedPack(group, redPack)
            RedPackRepository.delete(redPack)
            return
        }

        RedPackManager.getRedPack(sender, subject, redPack, message)
    }

    suspend fun queryRedPackList(event: GroupMessageEvent) {
        Log.info("红包查询指令")

        val subject = event.subject
        try {
            val group = event.group
            val bot = event.bot
            val redPacks = RedPackRepository.listByGroupId(group.id)

            val forwardMessage = ForwardMessageBuilder(subject)
            if (redPacks.isEmpty()) {
                subject.sendMessage("本群暂无红包！")
                return
            }
            RedPackManager.viewRedPack(subject, bot, redPacks, forwardMessage)
        } catch (e: Exception) {
            subject.sendMessage("查询失败! 原因: ${e.message}")
            throw RuntimeException(e)
        }
    }

    suspend fun grabNewestRedPack(event: GroupMessageEvent) {
        Log.info("抢红包指令")

        val subject = event.subject
        try {
            val group = event.group
            val sender = event.sender
            val redPacks = RedPackRepository.listByGroupId(group.id)
            if (redPacks.isEmpty()) {
                subject.sendMessage("当前群没有红包哦!")
                return
            }
            val sortedPacks = redPacks.sortedByDescending { it.createTime }
            RedPackManager.getRedPack(sender, subject, sortedPacks[0], event.message)
        } catch (e: Exception) {
            subject.sendMessage("领取失败! 原因: ${e.message}")
            throw RuntimeException(e)
        }
    }

    suspend fun queryGlobalRedPackList(event: MessageEvent) {
        Log.info("红包查看指令")

        val subject = event.subject
        try {
            val bot = event.bot
            val redPacks = RedPackRepository.listAll()

            val forwardMessage = ForwardMessageBuilder(subject)

            if (redPacks.isEmpty()) {
                subject.sendMessage("全局暂无红包！")
                return
            }
            RedPackManager.viewRedPack(subject, bot, redPacks, forwardMessage)
        } catch (e: Exception) {
            subject.sendMessage("查询失败! 原因: ${e.message}")
            throw RuntimeException(e)
        }
    }

    suspend fun startRob(event: GroupMessageEvent) {
        val group = event.group
        val util = PermUtil
        val user = UserUtil.group(group.id)

        if (util.checkUserHasPerm(user, EconPerm.RED_PACKET_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的红包已经开启了!"))
            return
        }

        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.RED_PACKET_PERM_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的红包开启成功!"))
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的红包开启失败!"))
        }
    }

    suspend fun endRob(event: GroupMessageEvent) {
        val group = event.group
        val user = UserUtil.group(group.id)

        if (!PermUtil.checkUserHasPerm(user, EconPerm.RED_PACKET_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的红包已经关闭!"))
            return
        }

        val permGroup = PermUtil.takePermGroupByName(EconPerm.GROUP.RED_PACKET_PERM_GROUP)
        permGroup.users.remove(user)
        permGroup.save()

        group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群的红包关闭成功!"))
    }
}


