package cn.chahuyun.economy.manager

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.redpack.RedPackDto
import cn.chahuyun.economy.model.redpack.RedPackKind
import cn.chahuyun.economy.utils.*
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
 * 绾㈠寘鐩稿叧鐨勨€滈潪浜嬩欢鐩戝惉鈥濋€昏緫銆?
 *
 * 璇存槑锛?
 * - `action.RedPackAction` 浠呬繚鐣欐寚浠ゅ叆鍙ｄ笌鍙傛暟瑙ｆ瀽銆?
 * - 杩欓噷璐熻矗锛氶殢鏈虹孩鍖呯畻娉曘€佺孩鍖呴鍙栦笌杩囨湡閫€杩樸€佺孩鍖呭垪琛ㄦ覆鏌撶瓑鍙鐢ㄩ€昏緫銆?
 */
object RedPackManager {

    /**
     * 浜屽€嶅潎鍊兼硶鐢熸垚闅忔満绾㈠寘鍒楄〃
     *
     * @param totalAmount 绾㈠寘鎬婚噾棰?
     * @param count 绾㈠寘涓暟
     * @return 鐢熸垚鐨勯殢鏈虹孩鍖呴噾棰濆垪琛?
     */
    @JvmStatic
    fun generateRandomPack(totalAmount: Double, count: Int): List<Double> {
        val result = mutableListOf<Double>()
        var remainingAmount = totalAmount
        var remainingCount = count

        // 閫愪釜鐢熸垚绾㈠寘閲戦锛屾渶鍚庝竴涓孩鍖呭崟鐙鐞?
        for (i in 0 until count - 1) {
            val avg = remainingAmount / remainingCount
            val max = avg * 2

            var amount = RandomUtil.randomDouble(0.1, max)
            amount = ShareUtils.rounding(amount)

            // 璁＄畻鍓╀綑绾㈠寘鐨勬渶灏忛鐣欓噾棰濓紝纭繚姣忎釜绾㈠寘鑷冲皯鏈?.1鍏?
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

            val nickNames = ArrayList<String>()
            for (receiver in receivers) {
                val member = group[receiver]
                val nameCard = member?.nameCard
                nickNames.add(nameCard ?: member?.nick ?: receiver.toString())
            }

            val typeStr = "【${type.description}】"
            val passwordStr = if (type == RedPackKind.PASSWORD) "\n绾㈠寘鍙ｄ护: $password" else ""

            val message = PlainText(
                "绾㈠寘淇℃伅 $typeStr: \n" +
                        "绾㈠寘ID: $id" +
                        "\n绾㈠寘鍚嶇О: $name" +
                        "\n绾㈠寘鍙戦€佽€? $senderId" +
                        "\n绾㈠寘鎬婚: ${MoneyFormatUtil.format(money)}" +
                        "\n鍓╀綑閲戦: ${MoneyFormatUtil.format(money - redPack.takenMoneys)}" +
                        "\n绾㈠寘浜烘暟: ${receivers.size}/$number" +
                        "\n鍒涘缓鏃堕棿: ${TimeConvertUtil.timeConvert(Date(createTime))}" +
                        passwordStr +
                        "\n宸查鍙栬€? $nickNames"
            )
            forwardMessage.add(bot, message)
        }
        subject.sendMessage(forwardMessage.build())
    }

    /**
     * 绾㈠寘棰嗗彇缁撴灉
     */
    data class GrabResult(
        val success: Boolean,
        val amount: Double = 0.0,
        val message: String = "",
        val finished: Boolean = false
    )

    /**
     * 鑾峰彇绾㈠寘锛堥鍙栭€昏緫锛?
     *
     * @param sender 棰嗗彇鑰?
     * @param redPack 绾㈠寘瀵硅薄
     * @param skipMessage 鏄惁璺宠繃鍙戦€侀€氱煡娑堟伅锛堢敤浜庢壒閲忛鍙栵級
     * @param passwordOverride 鍙ｄ护锛堝鏋滄彁渚涗笖鍖归厤锛屽垯鍏佽棰嗗彇鍙ｄ护绾㈠寘锛?
     * @return 棰嗗彇缁撴灉
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

        // 鍙ｄ护绾㈠寘鏍￠獙
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
            val msg = "浣犻鍙栦簡宸茬粡棰嗗畬鐨勭孩鍖咃紒"
            if (!skipMessage && message != null) subject.sendMessage(MessageUtil.formatMessageChain(message, msg))
            return GrabResult(false, message = msg)
        }

        // 棰嗗彇鎺柦
        val remainingRandomPacks = redPack.randomPackList.toMutableList()
        val perMoney: Double = if (redPack.isRandomAllocation) {
            if (remainingRandomPacks.isEmpty()) {
                throw RuntimeException("绾㈠寘宸茬粡琚骞插噣浜嗭紝浣嗕粛鐒跺湪棰嗗彇!")
            }
            val index = RandomUtil.randomInt(0, remainingRandomPacks.size)
            remainingRandomPacks.removeAt(index)
        } else {
            ShareUtils.rounding(money / number)
        }

        if (!EconomyUtil.plusMoneyToUser(sender, perMoney)) {
            val msg = "绾㈠寘棰嗗彇澶辫触!"
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
                    "鎭枩浣犻鍙栧埌浜嗕竴涓孩鍖咃紝浣犻鍙栦簡 ${MoneyFormatUtil.format(perMoney)} 鏋氶噾甯侊紒"
                )
            )
        }

        var finished = false
        if (savedRedPack.receiverList.size >= number) {
            val between = cn.hutool.core.date.DateUtil.formatBetween(
                Date(savedRedPack.createTime),
                Date(),
                cn.hutool.core.date.BetweenFormatter.Level.SECOND
            )
            if (!skipMessage) {
                subject.sendMessage(MessageUtil.formatMessageChain("${savedRedPack.name}宸茶棰嗗畬锛佸叡璁¤姳璐?{between}!"))
            }
            delete(savedRedPack)
            finished = true
        }

        return GrabResult(true, amount = perMoney, message = "棰嗗彇鎴愬姛", finished = finished)
    }

    /**
     * 绾㈠寘杩囨湡澶勭悊锛堥€€杩樺墿浣欓噾甯侊級
     */
    suspend fun expireRedPack(group: Group, redPack: RedPackDto) {
        val ownerId = redPack.sender
        val money = redPack.money

        val owner = group[ownerId]
        val remainingMoney = money - redPack.takenMoneys

        if (owner != null) {
            EconomyUtil.plusMoneyToUser(owner, remainingMoney)
        } else {
            // 缇ゅ唴鎵句笉鍒版垚鍛樻椂锛屾寜璐︽埛鐩存帴閫€鍥為挶鍖?
            val account = EconomyService.account(ownerId.toString(), null)
            EconomyUtil.plusMoneyToWalletForAccount(account, remainingMoney)
        }
        group.sendMessage(
            MessageUtil.formatMessageChain(ownerId, "浣犵殑绾㈠寘杩囨湡鍟︼紒閫€杩橀噾甯?${MoneyFormatUtil.format(remainingMoney)} 涓紒")
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


