package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.fish.FishBait
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.PropsShop
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import kotlin.math.ceil

/**
 * 道具商店事件
 *
 * @author Moyuyanli
 * @date 2024/9/25 10:40
 */
@EventComponent
class EventPropsAction {

    @MessageAuthorize(text = ["道具商店( \\d+)?"], messageMatching = MessageMatchingEnum.REGULAR)
    suspend fun viewShop(event: GroupMessageEvent) {
        val content = event.message.contentToString()

        if (content.matches(Regex("道具商店( \\d+)"))) {
            val i = content.split(" ")[1].toInt()
            viewShop(event, i)
        } else {
            viewShop(event, 1)
        }
    }

    suspend fun viewShop(event: GroupMessageEvent, page: Int) {
        var currentPage = page
        val bot = event.bot
        val sender = event.sender
        val group = event.subject

        // 获取所有商店信息
        val shopInfo = PropsShop.getShopInfo()
        val pageSize = 10

        // 计算总页数
        val totalItems = shopInfo.size
        val totalPages = ceil(totalItems.toDouble() / pageSize).toInt()

        // 检查请求的页数是否有效
        if (currentPage < 1 || (totalPages in 1..<currentPage)) {
            group.sendMessage("无效的页数: $currentPage")
            return
        }

        // 创建转发消息构建器
        val nodes = ForwardMessageBuilder(group)

        // 添加标题
        nodes.add(bot, PlainText("以下是道具商店↓:"))

        // 计算起始索引和结束索引
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = (startIndex + pageSize).coerceAtMost(totalItems)

        // 条数计数器
        var index = 0

        // 遍历商店信息并添加到转发消息中
        for ((key, value) in shopInfo) {
            if (index >= endIndex) break
            if (index >= startIndex) {
                val format = String.format("道具code:%s%n%s", key, value)
                nodes.add(bot, PlainText(format))
            }
            index++
        }

        // 添加页脚信息
        nodes.add(
            bot,
            PlainText(String.format("当前页数: %d / 总页数: %d ; 总条数: %d", currentPage, totalPages, totalItems))
        )

        // 发送消息
        group.sendMessage(nodes.build())

        while (true) {
            val nextMessage = MessageUtil.INSTANCE.nextUserForGroupMessageEventSync(group.id, sender.id, 180) ?: return
            val content = nextMessage.message.contentToString()
            if (content == "下一页") {
                if (++currentPage <= totalPages) {
                    viewShop(nextMessage, currentPage)
                } else {
                    group.sendMessage(MessageUtil.formatMessageChain(nextMessage.message, "没有下一页了"))
                }
            } else if (content == "上一页") {
                if (--currentPage > 0) {
                    viewShop(nextMessage, currentPage)
                } else {
                    group.sendMessage(MessageUtil.formatMessageChain(nextMessage.message, "没有上一页了"))
                }
            } else {
                return
            }
        }
    }

    @MessageAuthorize(
        text = ["buy( \\S+)+|购买道具( \\S+)+"],
        messageMatching = MessageMatchingEnum.REGULAR
    )
    suspend fun buyProp(event: GroupMessageEvent) {
        Log.info("购买指令")

        val group = event.subject
        val message = event.message
        val content = message.contentToString()
        val sender = event.sender

        val split = content.split(" ")
        val builder = MessageChainBuilder()
        builder.add(QuoteReply(message))
        builder.add("本次购买道具:")

        val userInfo = UserCoreManager.getUserInfo(sender)

        for (i in 1 until split.size) {
            var code = split[i]
            if (code.isBlank()) continue
            var number = 1
            if (code.matches(Regex("^\\S+\\*\\d+$"))) {
                val strings = code.split("*")
                code = strings[0]
                try {
                    number = strings[1].toInt().coerceAtMost(1000)
                } catch (_: NumberFormatException) {
                    group.sendMessage(MessageUtil.formatMessageChain(sender.id, "没办法买那么多!"))
                    return
                }
            }

            if (!PropsShop.checkPropExist(code) && !PropsShop.checkPropNameExist(code)) {
                builder.add(MessageUtil.formatMessage("\n道具 %s 不存在!", code))
                continue
            }

            var template = PropsShop.getTemplate(code)
            if (template == null) {
                template = PropsShop.getTemplateByName(code)
            }
            val name = template.name
            val propCode = template.code
            val money = EconomyUtil.getMoneyByUser(userInfo.user)
            val cost = template.cost
            val finalCost = cost * number

            if (money < finalCost) {
                builder.add(MessageUtil.formatMessage("\n道具 %s ,余额不足%d,购买失败!", name, finalCost))
                continue
            }

            if (EconomyUtil.minusMoneyToUser(userInfo.user, finalCost.toDouble())) {
                if (template is Stackable) {
                    if (BackpackAction.checkPropInUser(userInfo, propCode)) {
                        val baseNumber = FishBait.fishbaitTimer[propCode] ?: 1
                        val buyNum = number * baseNumber

                        val backpack = userInfo.getProp(propCode)
                        val prop = PropsManager.getProp(backpack)
                        if (prop is Stackable) {
                            (prop as Stackable).num += buyNum
                            backpack.propId?.let { PropsManager.updateProp(it, prop) }
                        }
                    } else {
                        val propId = PropsManager.addProp(template)
                        val prop = PropsManager.getProp(template.kind, propId)

                        val baseNumber = FishBait.fishbaitTimer[propCode] ?: 1
                        val buyNum = number * baseNumber

                        if (prop is Stackable) {
                            (prop as Stackable).num = buyNum
                            PropsManager.updateProp(propId, prop)
                        }
                        BackpackAction.addPropToBackpack(userInfo, propCode, template.kind, propId)
                    }
                } else {
                    repeat(number) {
                        val propId = PropsManager.addProp(template)
                        BackpackAction.addPropToBackpack(userInfo, propCode, template.kind, propId)
                    }
                }

                val unit = if (template is Stackable) (template as Stackable).unit else "个"
                builder.add(MessageUtil.formatMessage("\n道具 %s 购买 %d %s 成功!", name, number, unit))
            } else {
                builder.add(MessageUtil.formatMessage("\n道具 %s 购买失败!", name))
            }
        }

        group.sendMessage(builder.build())
    }
}
