package cn.chahuyun.economy.manager

import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.model.props.PropsCard
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.sign.SignRewardEvent
import cn.chahuyun.economy.utils.MessageUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText

object SignManager {

    /**
     * 签到金钱获取
     * 优先级: EventPriority.HIGH
     */
    @JvmStatic
    fun randomSignGold(event: SignRewardEvent) {
        var goldNumber: Double
        val builder = MessageChainBuilder()

        val param = event.param ?: 0

        if (param <= 500) {
            goldNumber = RandomUtil.randomInt(50, 101).toDouble()
        } else if (param <= 850) {
            goldNumber = RandomUtil.randomInt(100, 201).toDouble()
            builder.add(PlainText("哇偶,你今天运气爆棚,获得${goldNumber}金币"))
        } else if (param <= 999) {
            goldNumber = RandomUtil.randomInt(200, 501).toDouble()
            builder.add(PlainText("卧槽,你家祖坟裂了,冒出${goldNumber}金币"))
        } else {
            goldNumber = 999.0
            builder.add(PlainText("你™直接天降神韵!"))
        }

        event.baseReward = goldNumber
        event.reply = builder.build()
    }

    /**
     * 自定义签到事件
     * 优先级: EventPriority.NORMAL
     */
    @JvmStatic
    fun signProp(event: SignRewardEvent) {
        val userInfo: UserInfoDto = event.userInfo
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
                event.doubleCardApplied = true
                event.tripleCardApplied = true
                multiples += 4
                event.eventReplyAdd(MessageUtil.formatMessageChain("已启用签到月卡,本次签到奖励翻5倍!"))
            }
        }

        for (backpack in list) {
            val propId = backpack.propId
            val propCode = backpack.propCode
            when (propCode) {
                PropsCard.SIGN_2 -> {
                    val card = try {
                        PropsManager.deserialization(propId, PropsCard::class.java)
                    } catch (_: Exception) {
                        event.consumeProp(backpack)
                        continue
                    }
                    if (event.doubleCardApplied) {
                        continue
                    }
                    if (card.status) {
                        multiples += 1
                        event.consumeProp(backpack)
                        event.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张双倍签到卡，本次签到奖励翻倍!"))
                        event.doubleCardApplied = true
                    }
                }

                PropsCard.SIGN_3 -> {
                    val card = try {
                        PropsManager.deserialization(propId, PropsCard::class.java)
                    } catch (_: Exception) {
                        event.consumeProp(backpack)
                        continue
                    }
                    if (event.tripleCardApplied) {
                        continue
                    }
                    if (card.status) {
                        multiples += 2
                        event.consumeProp(backpack)
                        event.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张三倍签到卡，本次签到奖励三翻倍!"))
                        event.tripleCardApplied = true
                    }
                }

                PropsCard.SIGN_IN -> {
                    try {
                        PropsManager.deserialization(propId, PropsCard::class.java)
                    } catch (_: Exception) {
                        BackpackManager.delPropToBackpack(userInfo, propId)
                        continue
                    }
                    if (event.makeupCardApplied) {
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
                    event.makeupCardApplied = true
                }
            }
        }

        event.addMultiplier(multiples - 1)
    }
}
