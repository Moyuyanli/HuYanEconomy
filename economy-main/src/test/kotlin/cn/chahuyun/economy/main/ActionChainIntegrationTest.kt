package cn.chahuyun.economy.main

import cn.chahuyun.economy.actionchain.generated.EconomyCoreActionModule
import cn.chahuyun.economy.actionchain.generated.EconomyGameActionModule
import cn.chahuyun.economy.actionchain.runtime.ActionEngine
import cn.chahuyun.economy.actionchain.runtime.ActionResultHandler
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
        assertEquals(setOf("core.sign"), EconomyCoreActionModule.module.actionIds)

        assertEquals("economy-game", EconomyGameActionModule.module.moduleId)
        assertEquals(setOf("game.fish"), EconomyGameActionModule.module.actionIds)

        assertEquals(setOf("core.sign", "game.fish"), ActionChainBootstrap.registeredActionIds)
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
    fun `runtime can be installed and uninstalled`() {
        ActionRuntime.install(ActionEngine(listOf(EconomyCoreActionModule.module), ActionResultHandler { _, _ -> }))
        assertTrue(ActionRuntime.isInstalled)

        ActionChainBootstrap.shutdown()
        assertTrue(!ActionRuntime.isInstalled)
    }
}
