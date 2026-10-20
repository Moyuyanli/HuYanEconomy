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
        // 签到功能现在整体入链：主流程 + 关闭/开启/刷新三个管理动作。
        assertEquals(
            setOf("core.sign", "core.sign.close", "core.sign.open", "core.sign.refresh"),
            EconomyCoreActionModule.module.actionIds,
        )

        assertEquals("economy-game", EconomyGameActionModule.module.moduleId)
        assertEquals(setOf("game.fish"), EconomyGameActionModule.module.actionIds)

        assertEquals(
            setOf("core.sign", "core.sign.close", "core.sign.open", "core.sign.refresh", "game.fish"),
            ActionChainBootstrap.registeredActionIds,
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
    fun `runtime can be installed and uninstalled`() {
        ActionRuntime.install(ActionEngine(listOf(EconomyCoreActionModule.module), ActionResultHandler { _, _ -> }))
        assertTrue(ActionRuntime.isInstalled)

        ActionChainBootstrap.shutdown()
        assertTrue(!ActionRuntime.isInstalled)
    }
}
