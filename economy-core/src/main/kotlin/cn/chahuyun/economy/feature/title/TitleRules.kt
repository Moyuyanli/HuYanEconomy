package cn.chahuyun.economy.feature.title

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
import cn.chahuyun.economy.model.title.TitleTemplate
import cn.chahuyun.economy.plugin.TitleTemplateManager
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil

/**
 * 称号功能的规则链：`@Before` / `@After` 规则集合。
 *
 * ## core.title.buy 的执行顺序
 * ```
 * @Before(order=10) validatePurchase   模板存在？可购买？余额够？没买过？→ 放入 TitleTemplate
 * ------------------ TitleUsecase.buyTitle ------------------  扣款 → 授予 → 回复
 * @After(order=10)  logPurchased       购买留痕
 * ```
 *
 * 校验从动作里抽出来的好处：以后要做「称号打折」「限时称号」「购买前置任务」，
 * 在本文件按 order 插规则即可，`TitleUsecase` 一行都不用改。
 */
object TitleRules {

    /**
     * 前置规则（order=10）：购买称号的资格校验。
     *
     * 全部通过时把 [TitleTemplate] 放进上下文，动作本体直接取用，避免第二次遍历商店列表。
     * 任一条不满足就 `EndWithMessage` 结束本次调用——动作与后置规则都不会执行，也不会扣钱。
     *
     * 校验顺序与原实现一致：模板存在 → 价格可购买 → 余额足够 → 尚未拥有。
     *
     * @param context 本次调用的上下文，用于放入解析出的称号模板。
     * @param actor 本次称号指令的参与者快照。
     */
    @Before(actionId = "core.title.buy", order = 10)
    suspend fun validatePurchase(
        context: ActionContext,
        @FromProvider("title-actor") actor: TitleActor,
    ): ActionResult {
        // 步骤 1：商店整体为空时给出与原实现一致的提示。
        val canBuyTemplate = TitleTemplateManager.getCanBuyTemplate()
        if (canBuyTemplate.isEmpty()) {
            return ActionResult.EndWithMessage("没有称号售卖!")
        }

        // 步骤 2：从指令里取出称号名（第二段）。取不到等价于「没有这个称号」。
        val content = actor.message.contentToString()
        val titleName = content.split(" +").getOrNull(1)
            ?: return ActionResult.EndWithMessage("没有这个称号!")

        val template = canBuyTemplate.firstOrNull { it.titleName == titleName }
            ?: return ActionResult.EndWithMessage("没有这个称号!")

        // 步骤 3：模板存在但当前不可购买（price 为空）。
        val price = template.price
            ?: return ActionResult.EndWithMessage("该称号当前不可购买")

        // 步骤 4：余额校验——必须在扣款之前拦截，动作里不再重复判断。
        val moneyByUser = EconomyUtil.getMoneyByUser(actor.user)
        if (moneyByUser < price) {
            return ActionResult.EndWithMessage(
                "你的金币不够 ${MoneyFormatUtil.format(price)} ,无法购买 ${template.titleName} 称号!"
            )
        }

        // 步骤 5：已经拥有就不许重复买（原实现同样是拦在扣款之前）。
        if (TitleManager.checkTitleIsExist(actor.userInfo, template.templateCode)) {
            return ActionResult.EndWithMessage("你已经拥有 ${template.titleName} 称号!")
        }

        context.put(TitleTemplate::class, template)
        return ActionResult.Continue
    }

    /**
     * 后置规则（order=10）：购买留痕。
     *
     * 只读事件与上下文、不判断结果：扣款失败时也留一条，排查时更有用。
     *
     * @param context 本次调用的上下文。
     */
    @After(actionId = "core.title.buy", order = 10)
    suspend fun logPurchased(
        context: ActionContext,
        @FromProvider("title-actor") actor: TitleActor,
    ) {
        val template = context.get(TitleTemplate::class) ?: return
        Log.info("称号购买:用户(${actor.user.id}) 购买(${template.titleName}) 价格(${template.price})")
    }

    /**
     * 后置规则（order=10）：切换称号的审计日志。
     *
     * 切换会改动全部称号的 `status`，属于批量写入，必须留痕。
     *
     * @param context 本次调用的上下文。
     */
    @After(actionId = "core.title.switch", order = 10)
    suspend fun logSwitched(
        @FromProvider("title-actor") actor: TitleActor,
    ) {
        Log.info("称号切换:用户(${actor.user.id}) 指令(${actor.message.contentToString()})")
    }
}
