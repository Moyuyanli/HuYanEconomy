package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.entity.TitleInfo
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.title.TitleTemplate
import cn.chahuyun.economy.plugin.TitleTemplateManager
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply

/**
 * 称号管理
 */
@EventComponent
class TitleAction {

    /**
     * 查询拥有的称号
     */
    @MessageAuthorize(text = ["我的称号", "称号列表", "拥有称号"])
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
            builder.append(String.format("%d-%s%n", ++index, titleName))
        }
        if (index != 0) {
            subject.sendMessage(builder.build())
        } else {
            subject.sendMessage("你还没有称号!")
        }
    }

    /**
     * 查询拥有的称号
     */
    @MessageAuthorize(text = ["称号商店"])
    suspend fun viewCanByTitle(event: MessageEvent) {
        Log.info("查询称号商店指令")

        val builder = MessageChainBuilder()
        builder.append("可购买的称号如下:\n")
        val canBuyTemplate: List<TitleTemplate> = TitleTemplateManager.getCanBuyTemplate()
        for (template in canBuyTemplate) {
            builder.append(
                String.format(
                    "%s - %s 金币-有效期: %s%n",
                    template.titleName,
                    template.price,
                    if (template.validityPeriod > 0) template.validityPeriod.toString() + "天" else "永久"
                )
            )
        }
        event.subject.sendMessage(builder.build())
    }

    /**
     * 购买称号
     */
    @MessageAuthorize(text = ["购买称号 (\\S+)"], messageMatching = MessageMatchingEnum.REGULAR)
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
                val moneyByUser = EconomyUtil.getMoneyByUser(sender)
                if (moneyByUser < template.price) {
                    subject.sendMessage(
                        MessageUtil.formatMessageChain(
                            message,
                            "你的金币不够 %s ,无法购买 %s 称号!",
                            template.price,
                            template.titleName
                        )
                    )
                    return
                } else {
                    val userInfo: UserInfo = UserCoreManager.getUserInfo(sender)
                    if (TitleManager.checkTitleIsExist(userInfo, template.templateCode)) {
                        subject.sendMessage(
                            MessageUtil.formatMessageChain(
                                message,
                                "你已经拥有 %s 称号!",
                                template.titleName
                            )
                        )
                        return
                    }
                    if (EconomyUtil.minusMoneyToUser(sender, template.price)) {
                        if (TitleManager.addTitleInfo(userInfo, template.templateCode)) {
                            subject.sendMessage(
                                MessageUtil.formatMessageChain(
                                    message,
                                    "你以成功购买 %s 称号,有效期 %s ",
                                    template.titleName,
                                    if (template.validityPeriod <= 0) "无限" else template.validityPeriod.toString() + "天"
                                )
                            )
                        } else {
                            subject.sendMessage(
                                MessageUtil.formatMessageChain(
                                    message,
                                    "购买 %s 称号失败",
                                    template.titleName
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

    /**
     * 切换称号
     */
    @MessageAuthorize(text = ["切换称号 (\\d+)"], messageMatching = MessageMatchingEnum.REGULAR)
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
                subject.sendMessage(String.format("已切换称号为 %s ", titleInfo.name))
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
