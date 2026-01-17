package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.manager.EventPropsManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.fish.FishBait
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.PropsShop
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply

object EventPropsUsecase {

    suspend fun viewShop(event: GroupMessageEvent) {
        val content = event.message.contentToString()
        if (content.matches(Regex("道具商店( \\d+)"))) {
            val i = content.split(" ")[1].toInt()
            EventPropsManager.viewShop(event, i)
        } else {
            EventPropsManager.viewShop(event, 1)
        }
    }

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
                builder.add(MessageUtil.formatMessage("\n道具 ${code} 不存在!"))
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
                builder.add(MessageUtil.formatMessage("\n道具 ${name} ,余额不足${finalCost},购买失败!"))
                continue
            }

            if (EconomyUtil.minusMoneyToUser(userInfo.user, finalCost.toDouble())) {
                if (template is Stackable) {
                    if (BackpackManager.checkPropInUser(userInfo, propCode)) {
                        val baseNumber = FishBait.fishbaitTimer[propCode] ?: 1
                        val buyNum = number * baseNumber

                        val backpack = userInfo.getProp(propCode)
                        val prop = PropsManager.getProp(backpack)
                        if (prop is Stackable) {
                            prop.num += buyNum
                            backpack.propId?.let { PropsManager.updateProp(it, prop) }
                        }
                    } else {
                        val propId = PropsManager.addProp(template)
                        val prop = PropsManager.getProp(template.kind, propId)

                        val baseNumber = FishBait.fishbaitTimer[propCode] ?: 1
                        val buyNum = number * baseNumber

                        if (prop is Stackable) {
                            prop.num = buyNum
                            PropsManager.updateProp(propId, prop)
                        }
                        BackpackManager.addPropToBackpack(userInfo, propCode, template.kind, propId)
                    }
                } else {
                    repeat(number) {
                        val propId = PropsManager.addProp(template)
                        BackpackManager.addPropToBackpack(userInfo, propCode, template.kind, propId)
                    }
                }

                val unit = if (template is Stackable) template.unit else "个"
                builder.add(MessageUtil.formatMessage("\n道具 ${name} 购买 ${number} ${unit} 成功!"))
            } else {
                builder.add(MessageUtil.formatMessage("\n道具 ${name} 购买失败!"))
            }
        }

        group.sendMessage(builder.build())
    }
}


