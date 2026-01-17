@file:Suppress("DuplicatedCode")

package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.entity.redpack.RedPack
import cn.chahuyun.economy.entity.redpack.RedPackType
import cn.chahuyun.economy.manager.RedPackManager
import cn.chahuyun.economy.repository.RedPackRepository
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.buildMessageChain
import java.util.*

/**
 * 红包用例对象，处理群聊中的红包相关功能
 */
object RedPackUsecase {

    /**
     * 创建红包
     * @param event 群消息事件
     */
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

        var type = RedPackType.NORMAL
        var password: String? = null

        if (info.size >= 4) {
            val typeStr = info[3]
            if (typeStr == "sj" || typeStr == "随机") {
                type = RedPackType.RANDOM
            } else if (typeStr == "kl" || typeStr == "口令") {
                if (info.size < 5) {
                    @Suppress("SpellCheckingInspection")
                    subject.sendMessage(
                        MessageUtil.formatMessageChain(
                            message, "口令红包需要提供口令！例：发红包 10 5 口令 Ciallo～(∠・ω< )⌒★"
                        )
                    )
                    return
                }
                type = RedPackType.PASSWORD
                password = info.subList(4, info.size).joinToString(" ")

                // 校验口令：1~16个汉字及基本标点符号
                val regex = "^[\\s\\S]{1,32}$".toRegex()
                if (!regex.matches(password)) {
                    subject.sendMessage(
                        MessageUtil.formatMessageChain(
                            message, "口令不合法！必须是1~32个基本字符。"
                        )
                    )
                    return
                }
            }
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
            type = type,
            password = password,
            createTime = Date()
        )

        if (pack.isRandomAllocation) {
            val doubles = RedPackManager.generateRandomPack(money, number)
            pack.randomPackList = doubles.toMutableList()
        }

        val resultPack = RedPackRepository.saveInTransaction(pack)
        val id = resultPack?.id ?: 0

        if (id == 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "红包创建失败!"))
            return
        }

        val prefix = HuYanEconomy.config.prefix
        val typeDesc = type.description

        val claimMsg = if (type == RedPackType.PASSWORD) {
            "领取方式: 直接发送口令 [ $password ] 即可领取"
        } else {
            "领取命令: ${prefix.ifBlank { "" }}领红包 $id"
        }

        subject.sendMessage(
            MessageUtil.formatMessageChain(
                "${sender.nick} 发送了一个 ${typeDesc},快来抢红包吧！\n" +
                    "红包ID: ${id}\n" +
                    "红包金额: ${MoneyFormatUtil.format(money)} 枚金币\n" +
                    "红包个数: ${number}\n" +
                    "发送时间: ${TimeConvertUtil.timeConvert(pack.createTime ?: Date())}\n" +
                    "${claimMsg}"
            )
        )
    }

    /**
     * 领取红包
     * @param event 群消息事件
     */
    suspend fun receive(event: GroupMessageEvent) {
        Log.info("收红包指令")

        val subject = event.subject
        val group = event.group
        val sender = event.sender
        val message = event.message
        val content = message.contentToString()

        val info = content.split(" ").filter { it.isNotBlank() }

        // 判断是否带有前缀指令
        val hasPrefix = content.startsWith("领红包") || content.startsWith("收红包")

        if (hasPrefix) {
            if (info.size < 2) {
                // 只有前缀，提示输入ID或口令（因为只有抢红包才支持一键全抢）
                subject.sendMessage(MessageUtil.formatMessageChain(message, "请在指令后输入红包ID或口令！"))
                return
            }

            val idOrPassword = info[1]
            val id = idOrPassword.toIntOrNull()

            if (id != null) {
                // 是数字ID，领取指定红包
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

                // 如果是口令红包，不能通过ID领取
                if (redPack.type == RedPackType.PASSWORD) {
                    subject.sendMessage(MessageUtil.formatMessageChain(message, "这是口令红包，请直接发送口令来领取！"))
                    return
                }

                RedPackManager.getRedPack(sender, subject, redPack, message)
            } else {
                // 不是数字，尝试作为口令
                val password = info.subList(1, info.size).joinToString(" ")
                grabByPassword(event, password)
            }
        } else {
            // 没有前缀，说明是直接发送的口令（由 RedPackAction.grabByPassword 触发）
            grabByPassword(event, content.trim())
        }
    }

    /**
     * 通过口令领取红包
     * @param event 群消息事件
     * @param password 口令
     */
    private suspend fun grabByPassword(event: GroupMessageEvent, password: String) {
        val group = event.group
        val sender = event.sender
        val subject = event.subject

        val redPacks = RedPackRepository.listByGroupId(group.id)
            .filter { it.type == RedPackType.PASSWORD && it.password == password }

        if (redPacks.isEmpty()) {
            return // 没有匹配的口令红包，不做处理
        }

        val results = mutableListOf<Pair<String, Double>>()

        for (redPack in redPacks) {
            // 检查是否领过
            if (redPack.receiverList.contains(sender.id)) continue
            // 检查是否领完
            if ((redPack.number ?: 0) <= redPack.receiverList.size) continue
            // 检查是否过期
            if (DateUtil.between(redPack.createTime, Date(), DateUnit.DAY) >= 1) {
                RedPackManager.expireRedPack(group, redPack)
                RedPackRepository.delete(redPack)
                continue
            }

            val result =
                RedPackManager.getRedPack(sender, subject, redPack, skipMessage = true, passwordOverride = password)
            if (result.success) {
                results.add((redPack.name ?: "红包") to result.amount)
            }
        }

        if (results.size == 1) {
            val (name, amount) = results[0]
            val messageChain = buildMessageChain {
                +At(sender.id)
                +" $password"
                +"\n你抢 $name 抢到 ${MoneyFormatUtil.format(amount)}"
            }
            subject.sendMessage(messageChain)
        } else if (results.size > 1) {
            val messageChain = buildMessageChain {
                +At(sender.id)
                +"你本次抢到的口令红包:\n"
                results.forEach { (name, amount) ->
                    +"$name,你抢到了 ${MoneyFormatUtil.format(amount)}\n"
                }
            }
            subject.sendMessage(messageChain)
        }
    }

    /**
     * 一键抢红包功能
     * @param event 群消息事件
     */
    suspend fun grabNewestRedPack(event: GroupMessageEvent) {
        Log.info("一键抢红包")

        val subject = event.subject
        val sender = event.sender
        try {
            val group = event.group
            val redPacks =
                RedPackRepository.listByGroupId(group.id).filter { it.type != RedPackType.PASSWORD } // 默认不抢口令红包

            if (redPacks.isEmpty()) {
                subject.sendMessage("当前群没有可领取的红包哦!")
                return
            }

            val results = mutableListOf<Pair<String, Double>>()

            for (redPack in redPacks) {
                // 检查是否领过或者领完
                if (redPack.receiverList.contains(sender.id)) continue
                if ((redPack.number ?: 0) <= redPack.receiverList.size) continue

                // 检查是否过期
                if (DateUtil.between(redPack.createTime, Date(), DateUnit.DAY) >= 1) {
                    RedPackManager.expireRedPack(group, redPack)
                    RedPackRepository.delete(redPack)
                    continue
                }

                val result = RedPackManager.getRedPack(sender, subject, redPack, skipMessage = true)
                if (result.success) {
                    results.add((redPack.name ?: "红包") to result.amount)
                }
            }

            if (results.isEmpty()) {
                subject.sendMessage("你已经抢过群里所有的红包啦，或者红包已经领完/过期了！")
                return
            }

            val messageChain = buildMessageChain {
                +At(sender.id)
                +"你本次抢到的红包:\n"
                results.forEach { (name, amount) ->
                    +"$name,你抢到了 ${MoneyFormatUtil.format(amount)}\n"
                }
            }
            subject.sendMessage(messageChain)
        } catch (e: Exception) {
            subject.sendMessage("领取失败! 原因: ${e.message}")
            Log.error("一键抢红包异常", e)
        }
    }

    /**
     * 查询群内红包列表
     * @param event 群消息事件
     */
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

    /**
     * 查询全局红包列表
     * @param event 消息事件
     */
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

    /**
     * 开启红包功能
     * @param event 群消息事件
     */
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

    /**
     * 关闭红包功能
     * @param event 群消息事件
     */
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


