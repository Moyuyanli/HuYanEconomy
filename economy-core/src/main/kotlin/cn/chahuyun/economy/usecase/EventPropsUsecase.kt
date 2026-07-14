package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.manager.EventPropsManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.fish.FishBait
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.model.user.user
import cn.chahuyun.economy.prop.BaseProp
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

    private data class BuyItem(val code: String, val number: Int)

    private fun createStackableProp(
        userInfo: UserInfoDto,
        template: BaseProp,
        propCode: String,
        buyNum: Int,
        createdPropIds: MutableList<Long>,
    ) {
        val prop = template.copyProp<BaseProp>()
        if (prop !is Stackable || !prop.isStack) {
            error("道具模板不是可堆叠道具: code=$propCode")
        }
        prop.num = buyNum
        val propId = PropsManager.addProp(prop)
        createdPropIds += propId
        BackpackManager.addPropToBackpack(userInfo, propCode, template.kind, propId)
    }

    private fun addOrCreateStackableProp(
        userInfo: UserInfoDto,
        template: BaseProp,
        propCode: String,
        buyNum: Int,
        createdPropIds: MutableList<Long>,
    ) {
        val backpacks = userInfo.backpacks.filter { it.propCode == propCode }
        for (backpack in backpacks) {
            val prop = PropsManager.getProp(backpack)
            if (prop == null) {
                Log.error("清理失效背包道具: user=${userInfo.qq}, code=$propCode, backpackId=${backpack.id}, propId=${backpack.propId}")
                BackpackManager.delPropToBackpack(userInfo, backpack)
                continue
            }
            if (prop !is Stackable || !prop.isStack) {
                Log.error("清理类型异常背包道具: user=${userInfo.qq}, code=$propCode, backpackId=${backpack.id}, propId=${backpack.propId}")
                BackpackManager.delPropToBackpack(userInfo, backpack)
                continue
            }

            prop.num += buyNum
            PropsManager.updateProp(backpack.propId, prop)
            return
        }

        createStackableProp(userInfo, template, propCode, buyNum, createdPropIds)
    }

    private fun parseBuyItems(content: String): List<BuyItem> {
        val split = content.split(Regex("\\s+")).filter { it.isNotBlank() }
        val items = mutableListOf<BuyItem>()
        var index = 1

        while (index < split.size) {
            var code = split[index]
            var number = 1

            if (code.matches(Regex("^\\S+\\*\\d+$"))) {
                val strings = code.split("*", limit = 2)
                code = strings[0]
                number = strings[1].toIntOrNull()?.coerceIn(1, 1000) ?: 1
            } else {
                val nextNumber = split.getOrNull(index + 1)?.toIntOrNull()
                if (nextNumber != null) {
                    number = nextNumber.coerceIn(1, 1000)
                    index++
                }
            }

            items += BuyItem(code, number)
            index++
        }

        return items
    }

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

        val buyItems = parseBuyItems(content)
        val builder = MessageChainBuilder()
        builder.add(QuoteReply(message))
        builder.add("本次购买道具:")

        val userInfo = UserCoreManager.getUserInfo(sender)

        for ((code, number) in buyItems) {
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

            if (!EconomyUtil.minusMoneyToUser(userInfo.user, finalCost.toDouble())) {
                builder.add(MessageUtil.formatMessage("\n道具 ${name} 购买失败!"))
                continue
            }

            val createdPropIds = mutableListOf<Long>()
            try {
                val stackableTemplate = template as? Stackable
                if (stackableTemplate?.isStack == true) {
                    val baseNumber = FishBait.fishbaitTimer[propCode] ?: 1
                    val buyNum = number * baseNumber

                    addOrCreateStackableProp(userInfo, template, propCode, buyNum, createdPropIds)
                } else {
                    repeat(number) {
                        val prop = template.copyProp<BaseProp>()
                        val propId = PropsManager.addProp(prop)
                        createdPropIds += propId
                        BackpackManager.addPropToBackpack(userInfo, propCode, template.kind, propId)
                    }
                }

                val unit = if (stackableTemplate?.isStack == true) stackableTemplate.unit else "个"
                builder.add(MessageUtil.formatMessage("\n道具 ${name} 购买 ${number} ${unit} 成功!"))
            } catch (e: Exception) {
                Log.error("购买道具发放失败，准备清理并退款: user=${userInfo.qq}, code=$propCode, number=$number", e)
                createdPropIds.forEach { propId ->
                    runCatching { PropsManager.destroyProsAndBackpack(propId) }
                        .onFailure { Log.error("购买道具失败后清理道具失败: propId=$propId", it) }
                }
                val refunded = EconomyUtil.plusMoneyToUser(userInfo.user, finalCost.toDouble())
                val refundMessage = if (refunded) "已退款${finalCost}" else "退款失败，请联系管理员"
                builder.add(MessageUtil.formatMessage("\n道具 ${name} 购买失败，${refundMessage}"))
            }
        }

        group.sendMessage(builder.build())
    }
}
