package cn.chahuyun.economy.feature.title

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.model.title.TitleTemplate
import cn.chahuyun.economy.plugin.TitleTemplateManager
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply

/**
 * 称号功能的主用例：本功能对外暴露的**全部动作**都收在这里。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `core.title.list` | 我的称号 / 称号列表 / 拥有称号 | 无（纯查询） |
 * | `core.title.shop` | 称号商店 | 无（纯查询） |
 * | `core.title.buy` | 购买称号 名称 | `@Before` 资格校验；`@After` 购买日志 |
 * | `core.title.switch` | 切换称号 编号 | `@After` 审计日志 |
 *
 * 购买资格校验从动作本体挪到了 [TitleRules.validatePurchase]，动作只保留
 * 「扣款 → 授予 → 回复」这条不可跳过的流水，与签到（`core.sign`）的分工一致。
 */
object TitleUsecase {

    /**
     * 查看自己拥有的称号（`core.title.list`）。
     *
     * @param event 消息事件（群聊或好友）。
     */
    @Action("core.title.list")
    suspend fun viewTitleInfo(event: MessageEvent) {
        Log.info("查询称号指令")

        val subject: Contact = event.subject
        val id = event.sender.id

        val builder = MessageChainBuilder()
        builder.append(QuoteReply(event.source))

        val titleList = TitleManager.findByUser(id)
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

    /**
     * 查看称号商店（`core.title.shop`）。
     *
     * @param event 消息事件。
     */
    @Action("core.title.shop")
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

    /**
     * 购买称号（`core.title.buy`）。
     *
     * 模板解析、可购买性、余额与重复持有都由 [TitleRules.validatePurchase] 在
     * `@Before(10)` 里判定并把 [TitleTemplate] 放进上下文；能走到这里说明全部通过。
     *
     * @param event 消息事件（第一个参数固定为事件）。
     * @param context 本次规则链调用的上下文，读取前置规则放进去的称号模板。
     */
    @Action("core.title.buy")
    suspend fun buyTitle(event: MessageEvent, context: ActionContext) {
        Log.info("购买称号指令")

        val subject: Contact = event.subject
        val message: MessageChain = event.message
        val sender: User = event.sender

        val template = context.get(TitleTemplate::class)
        if (template == null) {
            // 正常情况下前置规则一定会放；真为空说明规则被改动过，记录后放弃。
            Log.warning("称号购买:前置规则未产出模板，本次购买终止（用户 ${sender.id}）")
            return
        }
        val price = template.price ?: return

        val actor = context.provide(TitleActor::class, TitleProvider.ACTOR)
        if (EconomyUtil.minusMoneyToUser(sender, price)) {
            if (TitleManager.addTitleInfo(actor.userInfo, template.templateCode)) {
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

    /**
     * 切换当前启用的称号（`core.title.switch`）。
     *
     * @param event 消息事件。
     */
    @Action("core.title.switch")
    suspend fun userTitle(event: MessageEvent) {
        Log.info("切换称号指令")

        val user: User = event.sender
        val subject: Contact = event.subject
        val message: MessageChain = event.message
        val content = message.contentToString()

        val split = content.split(" +")
        val i = split[1].toInt()

        val titleInfos = TitleManager.findByUser(user.id)
        if (titleInfos.isEmpty()) {
            subject.sendMessage("你的称号为空!")
            return
        }

        var index = 0
        for (titleInfo in titleInfos) {
            if (++index == i) {
                TitleManager.saveTitleInfo(titleInfo.copy(status = true))
                subject.sendMessage("已切换称号为 ${titleInfo.name} ")
            } else {
                TitleManager.saveTitleInfo(titleInfo.copy(status = false))
            }
        }

        if (i == 0) {
            subject.sendMessage("已切换为默认称号!")
        }
    }
}
