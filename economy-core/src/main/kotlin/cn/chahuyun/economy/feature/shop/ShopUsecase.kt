package cn.chahuyun.economy.feature.shop

import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.feature.backpack.BackpackManager
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

/**
 * 道具商店功能的主用例：浏览商店、购买道具。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `core.shop.view` | 道具商店 [页码] | `@Before(10)` 解析页码 |
 * | `core.shop.buy` | buy / 购买道具 <道具名\|code>[*数量] ... | `@Before(10)` 解析清单；`@After` 留痕 |
 *
 * 购买是「逐条扣款 + 逐条发放」，任一参数不合法（道具不存在、余额不足）只跳过这一条并继续，
 * 因此每个条码的失败原因都回给用户；扣款成功但发放失败时会清理已发放道具并退款。
 */
object ShopUsecase {

    /**
     * 查看道具商店（`core.shop.view`）。
     *
     * 分页交互（下一页/上一页）由 [ShopManager] 处理。
     *
     * @param event 群消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [ShopViewPlan] 与参与者快照。
     */
    @Action("core.shop.view")
    suspend fun viewShop(event: GroupMessageEvent, context: ActionContext) {
        Log.info("道具商店指令:用户(${event.sender.id})")

        val actor = context.provide(ShopActor::class, ShopProvider.ACTOR)
        val plan = context.get(ShopViewPlan::class) ?: ShopViewPlan(1)
        ShopManager.viewShop(event, plan.page)
    }

    /**
     * 购买道具（`core.shop.buy`）。
     *
     * 购买清单已由 [ShopRules.validateBuy] 解析完成，这里只按顺序处理每一条。
     *
     * @param event 群消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [ShopBuyPlan] 与参与者快照。
     */
    @Action("core.shop.buy")
    suspend fun buyProp(event: GroupMessageEvent, context: ActionContext) {
        Log.info("购买指令:用户(${event.sender.id})")

        val actor = context.provide(ShopActor::class, ShopProvider.ACTOR)
        val plan = context.get(ShopBuyPlan::class)
        if (plan == null) {
            Log.warning("道具商店:购买规则未产出清单，本次终止（用户 ${event.sender.id}）")
            return
        }

        val group = actor.group
        val message = actor.message
        val userInfo = actor.userInfo

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(message))
        builder.add("本次购买道具:")

        for (item in plan.items) {
            val code = item.code
            val number = item.number

            if (!PropsShop.checkPropExist(code) && !PropsShop.checkPropNameExist(code)) {
                builder.add(MessageUtil.formatMessage("\n道具 ${code} 不存在!"))
                continue
            }

            var template = PropsShop.getTemplate(code)
            if (template == null) {
                template = PropsShop.getTemplateByName(code)
            }
            if (template == null) {
                builder.add(MessageUtil.formatMessage("\n道具 ${code} 不存在!"))
                continue
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
                    addOrCreateStackableProp(userInfo, template, propCode, number, createdPropIds)
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

    /**
     * 给可堆叠道具加数量；背包里没有同类堆叠道具时新建一个。
     *
     * 顺手清理「背包条目存在但道具实例已丢」的脏数据。
     *
     * @param userInfo 买家资料。
     * @param template 道具模板。
     * @param propCode 道具 code。
     * @param buyNum 本次购买数量。
     * @param createdPropIds 记录新建的道具 id，用于失败时回滚。
     */
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

    /**
     * 新建一个可堆叠道具实例并放进背包。
     *
     * @param userInfo 买家资料。
     * @param template 道具模板。
     * @param propCode 道具 code。
     * @param buyNum 初始数量。
     * @param createdPropIds 记录新建的道具 id，用于失败时回滚。
     */
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
}
