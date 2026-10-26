package cn.chahuyun.economy.main

import cn.chahuyun.economy.actionchain.generated.EconomyCoreActionModule
import cn.chahuyun.economy.actionchain.generated.EconomyGameActionModule
import cn.chahuyun.economy.actionchain.runtime.ActionEngine
import cn.chahuyun.economy.actionchain.runtime.ActionResultHandler
import cn.chahuyun.economy.actionchain.runtime.ActionRuleStage
import cn.chahuyun.economy.actionchain.runtime.ActionRuntime
import cn.chahuyun.economy.plugin.ActionChainBootstrap
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 阶段四集成测试：验证 core/game 生成的模块描述能被 main 聚合成唯一引擎，
 * 并通过生成的 facade 所依赖的 [ActionRuntime] 完成一次真实调用。
 */
class ActionChainIntegrationTest {

    @AfterTest
    fun tearDown() {
        ActionRuntime.uninstall()
    }

    @Test
    fun `core and game modules are aggregated by main`() {
        assertEquals("economy-core", EconomyCoreActionModule.module.moduleId)
        // 签到是最早入链的完整功能；此后 title / status / backpack / transfer / user 陆续并入，
        // 因此这里断言「包含」而不是「等于」，新增 feature 不需要回来改测试。
        val coreActions = EconomyCoreActionModule.module.actionIds
        assertTrue(
            coreActions.containsAll(
                setOf("core.sign", "core.sign.close", "core.sign.open", "core.sign.refresh"),
            ),
            "签到四个动作必须在链上：$coreActions",
        )
        assertTrue(
            coreActions.containsAll(
                setOf(
                    "core.title.list", "core.title.shop", "core.title.buy", "core.title.switch",
                    "core.status.my", "core.status.home", "core.status.homeAt", "core.status.discharge",
                    "core.backpack.view", "core.backpack.use", "core.backpack.discard",
                    "core.transfer.user", "core.transfer.cheat",
                    "core.user.info", "core.user.infoText", "core.user.money",
                ),
            ),
            "第一批迁移的 feature 动作必须全部入链：$coreActions",
        )

        // 第二批：金融层。主银行、私银、国卷、红包、道具商店。
        assertTrue(
            coreActions.containsAll(
                setOf(
                    "core.bank.deposit", "core.bank.withdraw", "core.bank.rate", "core.bank.regalTop",
                    "core.bond.list", "core.bond.holdings", "core.bond.supplement",
                    "core.bond.buy", "core.bond.redeem",
                    "core.redpack.create", "core.redpack.receive", "core.redpack.grabByPassword",
                    "core.redpack.grabNewest", "core.redpack.list", "core.redpack.globalList",
                    "core.redpack.open", "core.redpack.close",
                    "core.shop.view", "core.shop.buy",
                ),
            ),
            "第二批迁移的金融 feature 动作必须全部入链：$coreActions",
        )
        assertTrue(
            coreActions.containsAll(
                setOf(
                    "core.privatebank.list", "core.privatebank.create", "core.privatebank.desc",
                    "core.privatebank.rate", "core.privatebank.profile", "core.privatebank.review",
                    "core.privatebank.fund", "core.privatebank.divest",
                    "core.privatebank.loan.offer", "core.privatebank.loan.rate",
                    "core.privatebank.loan.list", "core.privatebank.loan.cancel",
                    "core.privatebank.loan.borrowed", "core.privatebank.borrow", "core.privatebank.repay",
                    "core.privatebank.fox.view", "core.privatebank.fox.bid", "core.privatebank.fox.supplement",
                    "core.privatebank.info", "core.privatebank.my", "core.privatebank.myText",
                    "core.privatebank.default.show", "core.privatebank.default.set",
                ),
            ),
            "私银 23 个动作必须全部入链：$coreActions",
        )

        assertEquals("economy-game", EconomyGameActionModule.module.moduleId)
        // 第三批：game 模块的四个玩法整体迁入 games/；钓鱼按要求后期重建，当前只有占位动作。
        val gameActions = EconomyGameActionModule.module.actionIds
        assertTrue(
            gameActions.containsAll(
                setOf(
                    "game.farm.view", "game.farm.viewText", "game.farm.detail", "game.farm.level",
                    "game.farm.shop", "game.farm.warehouse", "game.farm.blackMarket",
                    "game.farm.buySeed", "game.farm.plant", "game.farm.harvest", "game.farm.sell",
                    "game.farm.upgrade", "game.farm.water", "game.farm.steal",
                    "game.farm.sellAll", "game.farm.harvestAll", "game.farm.plantAll", "game.farm.shield",
                    "game.lottery.bet", "game.lottery.open", "game.lottery.close",
                    "game.raffle.draw", "game.raffle.ten",
                    "game.rob.rob", "game.rob.hit", "game.rob.open", "game.rob.close",
                    "game.fish",
                ),
            ),
            "第三批迁移的玩法动作必须全部入链：$gameActions",
        )

        assertTrue(
            ActionChainBootstrap.registeredActionIds.containsAll(
                setOf(
                    "core.sign", "core.sign.close", "core.sign.open", "core.sign.refresh",
                    "core.title.buy", "core.status.discharge", "core.backpack.discard",
                    "core.transfer.user", "core.user.info",
                    "core.bank.deposit", "core.privatebank.borrow", "core.bond.buy",
                    "core.redpack.create", "core.shop.buy",
                    "game.farm.buySeed", "game.lottery.bet", "game.raffle.draw", "game.rob.rob",
                    "game.fish",
                ),
            ),
            "main 聚合后的 actionId 缺少已迁移功能：${ActionChainBootstrap.registeredActionIds}",
        )
        assertTrue(
            EconomyGameActionModule.module.rules.any { it.actionId == "game.fish" },
            "game.fish 至少注册一条 before 规则",
        )
    }

    @Test
    fun `engine accepts the aggregated modules and runs the fishing pilot rules`() = runBlocking {
        // 规则链在聚合期完成 actionId 全局唯一、规则引用、Provider 冲突校验，构造失败会直接抛异常。
        val engine: ActionEngine = ActionChainBootstrap.engine

        val execution = engine.invokeFromEntry("game.fish", "pilot-event") { "ok" }

        assertTrue(execution.actionExecuted, "before 规则应返回 Continue，动作需要被执行")
        assertEquals("ok", execution.value)
    }

    @Test
    fun `sign feature exposes before and after rules instead of a Mirai event`() = runBlocking {
        val signRules = EconomyCoreActionModule.module.rules.filter { it.actionId == "core.sign" }

        assertTrue(
            signRules.any { it.stage == ActionRuleStage.BEFORE },
            "core.sign 的签到资格校验与奖励计算应由 @Before 规则承载",
        )
        assertTrue(
            signRules.any { it.stage == ActionRuleStage.AFTER },
            "core.sign 的后置逻辑应由 @After 规则承载，不再有 SignCommittedEvent",
        )
        assertTrue(
            signRules.all { it.ruleId.startsWith("cn.chahuyun.economy.feature.sign.SignRules#") },
            "签到规则必须来自 feature/sign/SignRules.kt：${signRules.map { it.ruleId }}",
        )

        // 管理类动作没有 Provider 依赖，可以用假事件跑完整条链；
        // 其 @After 审计规则会对非群消息事件静默跳过，不应抛异常。
        val execution = ActionChainBootstrap.engine.invokeFromEntry("core.sign.refresh", "refresh-event") { }
        assertTrue(execution.actionExecuted, "after 规则不应打断动作执行")
    }

    @Test
    fun `privatebank guards live in rules instead of the action body`() {
        // 私银 23 个动作里，9 个需要行长身份、11 个需要参数解析；
        // 这些判定必须在 @Before 规则里，动作只做服务调用与回复。
        val pbRules = EconomyCoreActionModule.module.rules.filter { it.actionId.startsWith("core.privatebank.") }
        assertTrue(pbRules.isNotEmpty(), "私银必须注册规则")
        assertTrue(
            pbRules.all { it.ruleId.startsWith("cn.chahuyun.economy.feature.privatebank.PrivateBankRules#") },
            "私银规则必须来自 feature/privatebank/PrivateBankRules.kt：${pbRules.map { it.ruleId }}",
        )

        val descRules = pbRules.filter { it.actionId == "core.privatebank.desc" }
        assertTrue(
            descRules.any { it.stage == ActionRuleStage.BEFORE },
            "改描述的行长/破产/群聊校验应在 @Before：${descRules.map { it.ruleId }}",
        )

        val borrowRules = pbRules.filter { it.actionId == "core.privatebank.borrow" }
        assertTrue(
            borrowRules.any { it.stage == ActionRuleStage.BEFORE } &&
                borrowRules.any { it.stage == ActionRuleStage.AFTER },
            "借贷需要 before 解析参数 + after 留痕：${borrowRules.map { it.ruleId }}",
        )
    }

    @Test
    fun `game feature guards live in rules instead of the action body`() {
        // 第三批：四个玩法的拦截判定必须在各自的 Rules 里，动作只做编排与回复。
        val robRules = EconomyGameActionModule.module.rules.filter { it.actionId.startsWith("game.rob.") }
        assertTrue(
            robRules.all { it.ruleId.startsWith("cn.chahuyun.economy.games.rob.RobRules#") },
            "抢劫规则必须来自 games/rob/RobRules.kt：${robRules.map { it.ruleId }}",
        )
        val robAttemptRules = robRules.filter { it.actionId == "game.rob.rob" }
        assertTrue(
            robAttemptRules.count { it.stage == ActionRuleStage.BEFORE } >= 3,
            "欠贷 / 冷却 / 目标解析三条校验都应在 @Before：${robAttemptRules.map { it.ruleId }}",
        )

        val farmRules = EconomyGameActionModule.module.rules.filter { it.actionId.startsWith("game.farm.") }
        assertTrue(
            farmRules.all { it.ruleId.startsWith("cn.chahuyun.economy.games.farm.FarmRules#") },
            "农场规则必须来自 games/farm/FarmRules.kt：${farmRules.map { it.ruleId }}",
        )
        assertTrue(
            farmRules.any { it.actionId == "game.farm.water" && it.stage == ActionRuleStage.BEFORE },
            "帮浇水的 @目标 解析应在 @Before：${farmRules.map { it.ruleId }}",
        )

        val lotteryRules = EconomyGameActionModule.module.rules.filter { it.actionId.startsWith("game.lottery.") }
        assertTrue(
            lotteryRules.all { it.ruleId.startsWith("cn.chahuyun.economy.games.lottery.LotteryRules#") },
            "猜签规则必须来自 games/lottery/LotteryRules.kt：${lotteryRules.map { it.ruleId }}",
        )
        assertTrue(
            lotteryRules.count { it.actionId == "game.lottery.bet" && it.stage == ActionRuleStage.BEFORE } >= 3,
            "猜签的解析 / 余额 / 签种三道校验都应在 @Before：${lotteryRules.map { it.ruleId }}",
        )

        val raffleRules = EconomyGameActionModule.module.rules.filter { it.actionId.startsWith("game.raffle.") }
        assertTrue(
            raffleRules.all { it.ruleId.startsWith("cn.chahuyun.economy.games.raffle.RaffleRules#") },
            "抽奖规则必须来自 games/raffle/RaffleRules.kt：${raffleRules.map { it.ruleId }}",
        )
        assertTrue(
            raffleRules.count { it.stage == ActionRuleStage.BEFORE } >= 4,
            "单抽与十连各需要冷却 + 余额两条前置校验：${raffleRules.map { it.ruleId }}",
        )
    }

    @Test
    fun `runtime can be installed and uninstalled`() {
        ActionRuntime.install(ActionEngine(listOf(EconomyCoreActionModule.module), ActionResultHandler { _, _ -> }))
        assertTrue(ActionRuntime.isInstalled)

        ActionChainBootstrap.shutdown()
        assertTrue(!ActionRuntime.isInstalled)
    }
}
