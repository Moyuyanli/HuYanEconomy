package cn.chahuyun.economy.manager

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.entity.redpack.RedPack
import cn.chahuyun.economy.utils.*
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.BetweenFormatter
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import java.util.*

/**
 * 红包管理类，用于处理红包的创建、领取、查询等操作。
 */
@EventComponent
class RedPackManager {

    /**
     * 创建红包。
     *
     * @param event 群消息事件
     */
    @MessageAuthorize(
        text = ["发红包( \\d+){2}( (sj|随机))?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.RED_PACKET_PERM]
    )
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
            val doubles = generateRandomPack(money, number)
            pack.randomPackList = doubles.toMutableList()
        }

        val resultPack = HibernateFactory.getSessionFactory().fromTransaction {
            it.merge(pack)
        }
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

    /**
     * 二倍均值法生成随机红包列表
     */
    private fun generateRandomPack(totalAmount: Double, count: Int): List<Double> {
        val result = mutableListOf<Double>()
        var remainingAmount = totalAmount
        var remainingCount = count

        for (i in 0 until count - 1) {
            val avg = remainingAmount / remainingCount
            val max = avg * 2

            var amount = RandomUtil.randomDouble(0.1, max)
            amount = ShareUtils.rounding(amount)

            val minReserved = (remainingCount - 1) * 0.1
            if (remainingAmount - amount < minReserved) {
                amount = ShareUtils.rounding(remainingAmount - minReserved)
            }

            if (amount < 0.1) amount = 0.1

            result.add(amount)
            remainingAmount -= amount
            remainingCount--
        }
        result.add(ShareUtils.rounding(remainingAmount))
        return result
    }

    /**
     * 领取红包。
     *
     * @param event 群消息事件
     */
    @MessageAuthorize(
        text = ["领红包 \\d+|收红包 \\d+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.RED_PACKET_PERM]
    )
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

        val redPack = HibernateFactory.selectOneById<RedPack>(id)

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
            expireRedPack(group, redPack)
            HibernateFactory.delete(redPack)
            return
        }

        getRedPack(sender, subject, redPack, message)
    }

    /**
     * 查询红包列表。
     *
     * @param event 群消息事件
     */
    @MessageAuthorize(
        text = ["红包列表"],
        groupPermissions = [EconPerm.RED_PACKET_PERM]
    )
    suspend fun queryRedPackList(event: GroupMessageEvent) {
        Log.info("红包查询指令")

        val subject = event.subject
        try {
            val group = event.group
            val bot = event.bot
            val redPacks = HibernateFactory.selectList(RedPack::class.java, "groupId", group.id)

            val forwardMessage = ForwardMessageBuilder(subject)

            if (redPacks.isEmpty()) {
                subject.sendMessage("本群暂无红包！")
                return
            }

            viewRedPack(subject, bot, redPacks, forwardMessage)
        } catch (e: Exception) {
            subject.sendMessage("查询失败! 原因: ${e.message}")
            throw RuntimeException(e)
        }
    }

    private suspend fun viewRedPack(
        subject: Contact,
        bot: Bot,
        redPacks: List<RedPack>,
        forwardMessage: ForwardMessageBuilder,
    ) {
        if (subject !is Group) {
            return
        }
        val group = subject
        redPacks.forEach { redPack ->
            val id = redPack.id
            val name = redPack.name
            val senderId = redPack.sender
            val money = redPack.money
            val number = redPack.number
            val createTime = redPack.createTime
            val receivers = redPack.receiverList

            val nickNames = ArrayList<String>()
            for (receiver in receivers) {
                val member = group[receiver]
                val nameCard = member?.nameCard
                nickNames.add(nameCard ?: member?.nick ?: receiver.toString())
            }

            val message = PlainText(
                "红包信息: \n" +
                        "红包ID: $id" +
                        "\n红包名称: $name" +
                        "\n红包发送者QQ号: $senderId" +
                        "\n红包金币: $money" +
                        "\n剩余金币: ${String.format("%.1f", (money ?: 0.0) - redPack.takenMoneys)}" +
                        "\n红包人数: $number" +
                        "\n红包创建时间: ${TimeConvertUtil.timeConvert(createTime ?: Date())}" +
                        "\n红包领取者: $nickNames"
            )
            forwardMessage.add(bot, message)
        }
        subject.sendMessage(forwardMessage.build())
    }

    /**
     * 领取最新红包。
     *
     * @param event 消息事件
     */
    @MessageAuthorize(
        text = ["抢红包"],
        groupPermissions = [EconPerm.RED_PACKET_PERM]
    )
    suspend fun grabNewestRedPack(event: GroupMessageEvent) {
        Log.info("抢红包指令")

        val subject = event.subject
        try {
            val group = event.group
            val sender = event.sender
            val redPacks = HibernateFactory.selectList(RedPack::class.java, "groupId", group.id)
            if (redPacks.isEmpty()) {
                subject.sendMessage("当前群没有红包哦!")
                return
            }
            val sortedPacks = redPacks.sortedByDescending { it.createTime }

            getRedPack(sender, subject, sortedPacks[0], event.message)
        } catch (e: Exception) {
            subject.sendMessage("领取失败! 原因: ${e.message}")
            throw RuntimeException(e)
        }
    }

    /**
     * 获取红包。
     *
     * @param sender  发送者
     * @param subject 联系对象
     * @param redPack 红包
     */
    private suspend fun getRedPack(
        sender: User,
        subject: Contact,
        redPack: RedPack,
        message: net.mamoe.mirai.message.data.MessageChain,
    ) {
        val money = redPack.money
        val number = redPack.number ?: 1
        val isRandomPack = redPack.isRandomPack

        val receivers = redPack.receiverList
        if (receivers.isNotEmpty() && receivers.contains(sender.id)) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你已经领取过该红包了！"))
            return
        }

        if (receivers.size >= number) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你领取了已经领完的红包！"))
            return
        }

        // 领取措施
        val perMoney: Double = if (isRandomPack) {
            redPack.getRandomPack()
        } else {
            ShareUtils.rounding((money ?: 0.0) / number)
        }

        redPack.takenMoneys = redPack.takenMoneys + perMoney

        if (!EconomyUtil.plusMoneyToUser(sender, perMoney)) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "红包领取失败!"))
            return
        }

        receivers.add(sender.id)
        redPack.receiverList = receivers
        HibernateFactory.merge(redPack)

        subject.sendMessage(
            MessageUtil.formatMessageChain(
                message,
                "恭喜你领取到了一个红包，你领取了 %.1f 枚金币！",
                perMoney
            )
        )

        if (receivers.size >= number) {
            val between = DateUtil.formatBetween(redPack.createTime, Date(), BetweenFormatter.Level.SECOND)
            subject.sendMessage(MessageUtil.formatMessageChain("%s已被领完！共计花费%s!", redPack.name ?: "", between))
            HibernateFactory.delete(redPack)
        }
    }

    /**
     * 红包过期处理。
     *
     * @param group   群组
     * @param redPack 红包
     */
    suspend fun expireRedPack(group: Group, redPack: RedPack) {
        val ownerId = redPack.sender ?: return
        val money = redPack.money ?: 0.0

        val owner = group[ownerId]
        val remainingMoney = money - redPack.takenMoneys

        EconomyUtil.plusMoneyToUser(owner, remainingMoney)

        group.sendMessage(MessageUtil.formatMessageChain(ownerId, "你的红包过期啦！退还金币 %.1f 个！", remainingMoney))
    }

    /**
     * 查询全局红包列表。
     *
     * @param event 消息事件
     */
    @MessageAuthorize(
        text = ["全局红包列表"],
        userPermissions = [AuthPerm.OWNER]
    )
    suspend fun queryGlobalRedPackList(event: MessageEvent) {
        Log.info("红包查看指令")

        val subject = event.subject
        try {
            val bot = event.bot
            val redPacks = HibernateFactory.selectList(RedPack::class.java)

            val forwardMessage = ForwardMessageBuilder(subject)

            if (redPacks.isEmpty()) {
                subject.sendMessage("全局暂无红包！")
                return
            }

            viewRedPack(subject, bot, redPacks, forwardMessage)
        } catch (e: Exception) {
            subject.sendMessage("查询失败! 原因: ${e.message}")
            throw RuntimeException(e)
        }
    }

    @MessageAuthorize(
        text = ["开启 红包"],
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
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

    @MessageAuthorize(
        text = ["关闭 红包"],
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
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
