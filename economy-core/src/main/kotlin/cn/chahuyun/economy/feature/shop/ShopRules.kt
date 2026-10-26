package cn.chahuyun.economy.feature.shop

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.utils.Log

/**
 * 一次购买请求中的单个条码。
 *
 * @property code 道具 code（也允许写道具名，动作里会再按名称兜底匹配）。
 * @property number 购买数量（已夹到 1..1000）。
 */
data class ShopBuyItem(val code: String, val number: Int)

/**
 * 商店页码参数。
 *
 * @property page 要展示的页码（从 1 开始）。
 */
data class ShopViewPlan(val page: Int)

/**
 * 购买清单。
 *
 * @property items 本次要买的道具列表，顺序与用户输入一致。
 */
data class ShopBuyPlan(val items: List<ShopBuyItem>)

/**
 * 道具商店功能的规则链：`@Before` / `@After` 规则集合。
 *
 * ## 执行顺序
 * ```
 * @Before(order=10)  validateView   解析页码
 * @Before(order=10)  validateBuy    解析「道具[*数量]」清单
 * ------------------ ShopUsecase.viewShop / buyProp ------------------  列商品 / 逐条扣款发放
 * @After(order=10)   logBought      留痕
 * ```
 *
 * 购买清单解析放规则里，是因为它决定了「动作要处理哪几件事」：
 * 解析出空清单就没必要进动作，更不该在动作里重复一遍 `split/copyProp` 的模板代码。
 */
object ShopRules {

    /**
     * 前置规则（order=10）：解析 `道具商店 [页码]`。
     *
     * @param context 本次调用上下文，用于放入 [ShopViewPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.shop.view", order = 10)
    suspend fun validateView(
        context: ActionContext,
        @FromProvider("shop-actor") actor: ShopActor,
    ): ActionResult {
        val page = actor.content.split(" ").getOrNull(1)?.trim()?.toIntOrNull() ?: 1
        context.put(ShopViewPlan::class, ShopViewPlan(page))
        return ActionResult.Continue
    }

    /**
     * 前置规则（order=10）：解析购买清单。
     *
     * 支持三种写法：`购买道具 签到卡`、`购买道具 签到卡 3`、`购买道具 签到卡*3`。
     *
     * @param context 本次调用上下文，用于放入 [ShopBuyPlan]。
     * @param actor 本次指令的参与者快照。
     */
    @Before(actionId = "core.shop.buy", order = 10)
    suspend fun validateBuy(
        context: ActionContext,
        @FromProvider("shop-actor") actor: ShopActor,
    ): ActionResult {
        val items = parseBuyItems(actor.content)
        if (items.isEmpty()) {
            return ActionResult.EndWithMessage("用法：购买道具 <道具名|code>[*数量] ...")
        }
        context.put(ShopBuyPlan::class, ShopBuyPlan(items))
        return ActionResult.Continue
    }

    /**
     * 后置规则（order=10）：购买留痕。
     *
     * 逐条成功/失败由动作回给用户，这里只记「谁买了哪些 code」，
     * 便于和 `PropsManager` 的道具流水对账。
     *
     * @param context 本次调用上下文。
     * @param actor 本次指令的参与者快照。
     */
    @After(actionId = "core.shop.buy", order = 10)
    suspend fun logBought(
        context: ActionContext,
        @FromProvider("shop-actor") actor: ShopActor,
    ) {
        val plan = context.get(ShopBuyPlan::class) ?: return
        val detail = plan.items.joinToString(", ") { "${it.code}x${it.number}" }
        Log.info("道具商店:购买 用户(${actor.sender.id}) 清单($detail)")
    }

    /**
     * 解析「道具 [数量]」序列。
     *
     * 第一个词是指令本身（`buy` / `购买道具`），从第二个词开始才是道具。
     * 数量夹在 1..1000，避免一次买空商店或整数溢出。
     *
     * @param content 指令纯文本。
     * @return 购买清单；没有有效道具时返回空列表。
     */
    private fun parseBuyItems(content: String): List<ShopBuyItem> {
        val split = content.split(Regex("\\s+")).filter { it.isNotBlank() }
        val items = mutableListOf<ShopBuyItem>()
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

            items += ShopBuyItem(code, number)
            index++
        }

        return items
    }
}
