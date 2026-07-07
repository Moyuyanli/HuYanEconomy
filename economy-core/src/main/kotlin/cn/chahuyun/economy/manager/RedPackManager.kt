package cn.chahuyun.economy.manager

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.redpack.RedPackDto
import cn.chahuyun.economy.model.redpack.RedPackKind
import cn.chahuyun.economy.service.RedPackAllocationService
import cn.chahuyun.economy.service.isRandomAllocation
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.chahuyun.economy.utils.TimeConvertUtil
import cn.hutool.core.date.BetweenFormatter
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import xyz.cssxsh.mirai.economy.EconomyService
import java.util.*

/**
 * 红包管理器。
 *
 * 负责随机红包金额分配、红包领取、过期退还和红包列表展示。
 */
object RedPackManager {

    suspend fun viewRedPack(
        subject: Contact,
        bot: Bot,
        redPacks: List<RedPackDto>,
        forwardMessage: ForwardMessageBuilder,
    ) {
        if (subject !is Group) return
        val group = subject

        redPacks.forEach { redPack ->
            val id = redPack.id
            val name = redPack.name
            val senderId = redPack.sender
            val money = redPack.money
            val number = redPack.number
            val createTime = redPack.createTime
            val receivers = redPack.receiverList
            val type = redPack.type
            val password = redPack.password

            val nickNames = mutableListOf<String>()
            for (receiver in receivers) {
                val member = group[receiver]
                val nameCard = member?.nameCard
                nickNames.add(nameCard ?: member?.nick ?: receiver.toString())
            }

            val typeStr = "【${type.description}】"
            val passwordStr = if (type == RedPackKind.PASSWORD) "\n红包口令: $password" else ""

            val message = PlainText(
                "红包信息 $typeStr:\n" +
                    "红包 ID: $id" +
                    "\n红包名称: $name" +
                    "\n红包发送者: $senderId" +
                    "\n红包总额: ${MoneyFormatUtil.format(money)}" +
                    "\n剩余金额: ${MoneyFormatUtil.format(money - redPack.takenMoneys)}" +
                    "\n红包人数: ${receivers.size}/$number" +
                    "\n创建时间: ${TimeConvertUtil.timeConvert(Date(createTime))}" +
                    passwordStr +
                    "\n已领取者: $nickNames"
            )
            forwardMessage.add(bot, message)
        }
        subject.sendMessage(forwardMessage.build())
    }

    /**
     * 红包领取结果。
     */
    data class GrabResult(
        val success: Boolean,
        val amount: Double = 0.0,
        val message: String = "",
        val finished: Boolean = false
    )

    /**
     * 领取红包。
     *
     * @param sender 领取者
     * @param subject 消息上下文
     * @param redPack 红包对象
     * @param message 原始消息，用于引用回复
     * @param skipMessage 是否跳过通知消息，用于批量领取
     * @param passwordOverride 口令，匹配时允许领取口令红包
     * @return 领取结果
     */
    suspend fun getRedPack(
        sender: User,
        subject: Contact,
        redPack: RedPackDto,
        message: MessageChain? = null,
        skipMessage: Boolean = false,
        passwordOverride: String? = null
    ): GrabResult {
        val money = redPack.money
        val number = redPack.number
        val type = redPack.type

        if (type == RedPackKind.PASSWORD) {
            if (passwordOverride == null || passwordOverride != redPack.password) {
                return GrabResult(false, message = "这是口令红包，需要正确的口令才能领取。")
            }
        }

        val receivers = redPack.receiverList
        if (receivers.isNotEmpty() && receivers.contains(sender.id)) {
            val msg = "你已经领取过该红包了。"
            if (!skipMessage && message != null) subject.sendMessage(MessageUtil.formatMessageChain(message, msg))
            return GrabResult(false, message = msg)
        }

        if (receivers.size >= number) {
            val msg = "你领取的是已经领完的红包！"
            if (!skipMessage && message != null) subject.sendMessage(MessageUtil.formatMessageChain(message, msg))
            return GrabResult(false, message = msg)
        }

        val remainingRandomPacks = redPack.randomPackList.toMutableList()
        val perMoney: Double = if (redPack.isRandomAllocation) {
            RedPackAllocationService.drawRandomPack(remainingRandomPacks)
        } else {
            RedPackAllocationService.averagePack(money, number)
        }

        if (!EconomyUtil.plusMoneyToUser(sender, perMoney)) {
            val msg = "红包领取失败!"
            if (!skipMessage && message != null) subject.sendMessage(MessageUtil.formatMessageChain(message, msg))
            return GrabResult(false, message = msg)
        }

        val savedRedPack = save(
            redPack.copy(
                takenMoneys = redPack.takenMoneys + perMoney,
                receiverList = receivers + sender.id,
                randomPackList = remainingRandomPacks
            )
        )

        if (!skipMessage && message != null) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    message,
                    "恭喜你领取到了一个红包，你领取了 ${MoneyFormatUtil.format(perMoney)} 枚金币！"
                )
            )
        }

        var finished = false
        if (savedRedPack.receiverList.size >= number) {
            val between = DateUtil.formatBetween(
                Date(savedRedPack.createTime),
                Date(),
                BetweenFormatter.Level.SECOND
            )
            if (!skipMessage) {
                subject.sendMessage(MessageUtil.formatMessageChain("${savedRedPack.name}已被领完，共计耗时 $between!"))
            }
            delete(savedRedPack)
            finished = true
        }

        return GrabResult(true, amount = perMoney, message = "领取成功", finished = finished)
    }

    /**
     * 红包过期处理，退还剩余金币。
     */
    suspend fun expireRedPack(group: Group, redPack: RedPackDto) {
        val ownerId = redPack.sender
        val money = redPack.money

        val owner = group[ownerId]
        val remainingMoney = money - redPack.takenMoneys

        if (owner != null) {
            EconomyUtil.plusMoneyToUser(owner, remainingMoney)
        } else {
            // 群内找不到成员时，按账号直接退回钱包。
            val account = EconomyService.account(ownerId.toString(), null)
            EconomyUtil.plusMoneyToWalletForAccount(account, remainingMoney)
        }
        group.sendMessage(
            MessageUtil.formatMessageChain(ownerId, "你的红包过期啦！退还金币 ${MoneyFormatUtil.format(remainingMoney)} 枚！")
        )
    }

    fun findById(id: Int): RedPackDto? = redPackProxy.findById(id.toLong())

    fun listByGroupId(groupId: Long): List<RedPackDto> = redPackProxy.findWhere { it.groupId == groupId }

    fun listAll(): List<RedPackDto> = redPackProxy.findAll()

    fun save(redPack: RedPackDto): RedPackDto = redPackProxy.save(redPack)

    fun delete(redPack: RedPackDto): Boolean = redPackProxy.delete(redPack.id.toLong())

    private val redPackProxy
        get() = EntityProxyRegistry.get<RedPackDto>("redpack") ?: error("红包代理器未初始化")
}
