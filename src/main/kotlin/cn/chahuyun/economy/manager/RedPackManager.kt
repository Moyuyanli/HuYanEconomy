package cn.chahuyun.economy.manager

import cn.chahuyun.economy.entity.redpack.RedPack
import cn.chahuyun.economy.repository.RedPackRepository
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.ShareUtils
import cn.chahuyun.economy.utils.TimeConvertUtil
import cn.hutool.core.util.RandomUtil
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
 * 红包相关的“非事件监听”逻辑。
 *
 * 说明：
 * - `action.RedPackAction` 仅保留指令入口与参数解析。
 * - 这里负责：随机红包算法、红包领取与过期退还、红包列表渲染等可复用逻辑。
 */
object RedPackManager {

    /**
     * 二倍均值法生成随机红包列表
     */
    @JvmStatic
    fun generateRandomPack(totalAmount: Double, count: Int): List<Double> {
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

    suspend fun viewRedPack(
        subject: Contact,
        bot: Bot,
        redPacks: List<RedPack>,
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

            val nickNames = ArrayList<String>()
            for (receiver in receivers) {
                val member = group[receiver]
                val nameCard = member?.nameCard
                nickNames.add(nameCard ?: member?.nick ?: receiver.toString())
            }

            val typeStr = "【${type.description}】"
            val passwordStr = if (type == cn.chahuyun.economy.entity.redpack.RedPackType.PASSWORD) "\n红包口令: $password" else ""

            val message = PlainText(
                "红包信息 $typeStr: \n" +
                        "红包ID: $id" +
                        "\n红包名称: $name" +
                        "\n红包发送者: $senderId" +
                        "\n红包总额: $money" +
                        "\n剩余金额: ${String.format("%.1f", (money ?: 0.0) - redPack.takenMoneys)}" +
                        "\n红包人数: ${receivers.size}/$number" +
                        "\n创建时间: ${TimeConvertUtil.timeConvert(createTime ?: Date())}" +
                        passwordStr +
                        "\n已领取者: $nickNames"
            )
            forwardMessage.add(bot, message)
        }
        subject.sendMessage(forwardMessage.build())
    }

    /**
     * 红包领取结果
     */
    data class GrabResult(
        val success: Boolean,
        val amount: Double = 0.0,
        val message: String = "",
        val finished: Boolean = false
    )

    /**
     * 获取红包（领取逻辑）
     *
     * @param sender 领取者
     * @param redPack 红包对象
     * @param skipMessage 是否跳过发送通知消息（用于批量领取）
     * @param passwordOverride 口令（如果提供且匹配，则允许领取口令红包）
     * @return 领取结果
     */
    suspend fun getRedPack(
        sender: User,
        subject: Contact,
        redPack: RedPack,
        message: MessageChain? = null,
        skipMessage: Boolean = false,
        passwordOverride: String? = null
    ): GrabResult {
        val money = redPack.money
        val number = redPack.number ?: 1
        val type = redPack.type

        // 口令红包校验
        if (type == cn.chahuyun.economy.entity.redpack.RedPackType.PASSWORD) {
            if (passwordOverride == null || passwordOverride != redPack.password) {
                return GrabResult(false, message = "这是口令红包，需要正确的口令才能领取！")
            }
        }

        val receivers = redPack.receiverList
        if (receivers.isNotEmpty() && receivers.contains(sender.id)) {
            val msg = "你已经领取过该红包了！"
            if (!skipMessage && message != null) subject.sendMessage(MessageUtil.formatMessageChain(message, msg))
            return GrabResult(false, message = msg)
        }

        if (receivers.size >= number) {
            val msg = "你领取了已经领完的红包！"
            if (!skipMessage && message != null) subject.sendMessage(MessageUtil.formatMessageChain(message, msg))
            return GrabResult(false, message = msg)
        }

        // 领取措施
        val perMoney: Double = if (redPack.isRandomAllocation) {
            redPack.getRandomPack()
        } else {
            ShareUtils.rounding((money ?: 0.0) / number)
        }

        redPack.takenMoneys = redPack.takenMoneys + perMoney

        if (!EconomyUtil.plusMoneyToUser(sender, perMoney)) {
            val msg = "红包领取失败!"
            if (!skipMessage && message != null) subject.sendMessage(MessageUtil.formatMessageChain(message, msg))
            return GrabResult(false, message = msg)
        }

        receivers.add(sender.id)
        redPack.receiverList = receivers
        RedPackRepository.save(redPack)

        if (!skipMessage && message != null) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    message,
                    "恭喜你领取到了一个红包，你领取了 %.1f 枚金币！",
                    perMoney
                )
            )
        }

        var finished = false
        if (receivers.size >= number) {
            val between = cn.hutool.core.date.DateUtil.formatBetween(
                redPack.createTime,
                Date(),
                cn.hutool.core.date.BetweenFormatter.Level.SECOND
            )
            if (!skipMessage) {
                subject.sendMessage(MessageUtil.formatMessageChain("%s已被领完！共计花费%s!", redPack.name ?: "", between))
            }
            RedPackRepository.delete(redPack)
            finished = true
        }

        return GrabResult(true, amount = perMoney, message = "领取成功", finished = finished)
    }

    /**
     * 红包过期处理（退还剩余金币）
     */
    suspend fun expireRedPack(group: Group, redPack: RedPack) {
        val ownerId = redPack.sender ?: return
        val money = redPack.money ?: 0.0

        val owner = group[ownerId]
        val remainingMoney = money - redPack.takenMoneys

        if (owner != null) {
            EconomyUtil.plusMoneyToUser(owner, remainingMoney)
        } else {
            // 群内找不到成员时，按账户直接退回钱包
            val account = EconomyService.account(ownerId.toString(), null)
            EconomyUtil.plusMoneyToWalletForAccount(account, remainingMoney)
        }
        group.sendMessage(MessageUtil.formatMessageChain(ownerId, "你的红包过期啦！退还金币 %.1f 个！", remainingMoney))
    }
}


