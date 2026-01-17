package cn.chahuyun.economy.usecase

import cn.chahuyun.economy.entity.TitleInfo
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.title.TitleTemplate
import cn.chahuyun.economy.plugin.TitleTemplateManager
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply

object TitleUsecase {

    suspend fun viewTitleInfo(event: MessageEvent) {
        Log.info("查询称号指令")

        val subject: Contact = event.subject
        val id = event.sender.id

        val builder = MessageChainBuilder()
        builder.append(QuoteReply(event.source))

        val titleList = HibernateFactory.selectList(TitleInfo::class.java, "userId", id)
        if (titleList.isEmpty()) {
            subject.sendMessage(builder.append("你还没有称号!").build())
            return
        }

        builder.append("你拥有的称号如下:\n")
        var index = 0
        for (titleInfo in titleList) {
            if (TitleManager.checkTitleTime(titleInfo)) {
                continue
            }
            var titleName = titleInfo.name
            if (titleInfo.status) {
                titleName += ":已启用"
            }
            builder.append("${++index}-${titleName}\n")
        }
        if (index != 0) {
            subject.sendMessage(builder.build())
        } else {
            subject.sendMessage("你还没有称号!")
        }
    }

    suspend fun viewCanByTitle(event: MessageEvent) {
        Log.info("查询称号商店指令")

        val builder = MessageChainBuilder()
        builder.append("可购买的称号如下:\n")
        val canBuyTemplate: List<TitleTemplate> = TitleTemplateManager.getCanBuyTemplate()
        for (template in canBuyTemplate) {
            val price = template.price ?: 0.0
            val validity = template.validityPeriod ?: -1
            builder.append(
                "${template.titleName} - ${MoneyFormatUtil.format(price)} 金币-有效期: ${if (validity > 0) validity.toString() + "天" else "永久"}\n"
            )
        }
        event.subject.sendMessage(builder.build())
    }

    suspend fun buyTitle(event: MessageEvent) {
        Log.info("购买称号指令")

        val subject: Contact = event.subject
        val message: MessageChain = event.message

        val canBuyTemplate = TitleTemplateManager.getCanBuyTemplate()
        if (canBuyTemplate.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "没有称号售卖!"))
            return
        }

        val content = message.contentToString()
        val sender: User = event.sender
        for (template in canBuyTemplate) {
            if (template.titleName == content.split(" +")[1]) {
                val price = template.price
                if (price == null) {
                    subject.sendMessage(MessageUtil.formatMessageChain(message, "该称号当前不可购买"))
                    return
                }
                val moneyByUser = EconomyUtil.getMoneyByUser(sender)
                if (moneyByUser < price) {
                    subject.sendMessage(
                        MessageUtil.formatMessageChain(
                            message,
                            "你的金币不够 ${MoneyFormatUtil.format(price)} ,无法购买 ${template.titleName} 称号!"
                        )
                    )
                    return
                } else {
                    val userInfo: UserInfo = UserCoreManager.getUserInfo(sender)
                    if (TitleManager.checkTitleIsExist(userInfo, template.templateCode)) {
                        subject.sendMessage(
                            MessageUtil.formatMessageChain(
                                message,
                                "你已经拥有 ${template.titleName} 称号!"
                            )
                        )
                        return
                    }
                    if (EconomyUtil.minusMoneyToUser(sender, price)) {
                        if (TitleManager.addTitleInfo(userInfo, template.templateCode)) {
                            val validity = template.validityPeriod ?: -1
                            subject.sendMessage(
                                MessageUtil.formatMessageChain(
                                    message,
                                    "你以成功购买 ${template.titleName} 称号,有效期 ${if (validity <= 0) "无限" else validity.toString() + "天"} "
                                )
                            )
                        } else {
                            subject.sendMessage(
                                MessageUtil.formatMessageChain(
                                    message,
                                    "购买 ${template.titleName} 称号失败"
                                )
                            )
                        }
                    }
                }
                return
            }
        }
        subject.sendMessage(MessageUtil.formatMessageChain(message, "没有这个称号!"))
    }

    suspend fun userTitle(event: MessageEvent) {
        Log.info("切换称号指令")

        val user: User = event.sender
        val subject: Contact = event.subject
        val message: MessageChain = event.message
        val content = message.contentToString()

        val split = content.split(" +")
        val i = split[1].toInt()

        val titleInfos = HibernateFactory.selectList(TitleInfo::class.java, "userId", user.id)
        if (titleInfos.isEmpty()) {
            subject.sendMessage("你的称号为空!")
            return
        }

        var index = 0
        for (titleInfo in titleInfos) {
            if (++index == i) {
                titleInfo.status = true
                HibernateFactory.merge(titleInfo)
                subject.sendMessage("已切换称号为 ${titleInfo.name} ")
            } else {
                titleInfo.status = false
                HibernateFactory.merge(titleInfo)
            }
        }

        if (i == 0) {
            subject.sendMessage("已切换为默认称号!")
        }
    }
}
