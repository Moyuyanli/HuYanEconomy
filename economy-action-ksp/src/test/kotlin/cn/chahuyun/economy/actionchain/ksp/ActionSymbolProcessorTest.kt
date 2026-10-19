package cn.chahuyun.economy.actionchain.ksp

import com.tschuchort.compiletesting.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionSymbolProcessorTest {

    private companion object {
        const val RUNTIME_IMPORTS = """
import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.Before
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.actionchain.annotation.Provider
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.actionchain.runtime.ActionResult
"""
    }

    @Test
    fun `generates facade and module descriptor for a valid object`() {
        val (compilation, result) = compile(
            """
package fixture

$RUNTIME_IMPORTS

data class Account(val id: Long)
class TestEvent(val userId: Long)

object TransferUsecase {
    @Action("bank.transfer")
    suspend fun transfer(event: TestEvent, ctx: ActionContext) {
        ctx.put(Account::class, Account(7))
    }
}

object TransferRules {
    @Provider(qualifier = "fromUser", actionId = "bank.transfer")
    suspend fun fromAccount(ctx: ActionContext): Account = Account(1)

    @Before(actionId = "bank.transfer", order = 10)
    suspend fun validate(@FromProvider("fromUser") from: Account) {
        require(from.id > 0)
    }

    @After(actionId = "bank.transfer", order = 200)
    suspend fun settle(ctx: ActionContext): ActionResult = ActionResult.Continue
}
""",
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generated = compilation.generatedSources()
        val facade = generated.getValue("TransferUsecaseActions.kt")
        assertTrue(facade.contains("public suspend fun transfer(event: fixture.TestEvent)"), facade)
        assertTrue(facade.contains("ActionRuntime.invoke("), facade)
        assertTrue(facade.contains("TransferUsecase.transfer(event, context)"), facade)

        val moduleFile = generated.getValue("FixtureActionModule.kt")
        assertTrue(moduleFile.contains("moduleId = \"fixture\""), moduleFile)
        assertTrue(moduleFile.contains("setOf(\"bank.transfer\")"), moduleFile)
        assertTrue(moduleFile.contains("ActionRule.before("), moduleFile)
        assertTrue(moduleFile.contains("ActionRule.after("), moduleFile)
        assertTrue(moduleFile.contains("context.provide(fixture.Account::class, \"fromUser\")"), moduleFile)
        assertTrue(moduleFile.contains("actionProvider<fixture.Account>("), moduleFile)
    }

    @Test
    fun `rejects duplicate actionId`() {
        val (_, result) = compile(
            """
package fixture

$RUNTIME_IMPORTS

class TestEvent

object First {
    @Action("dup.action")
    suspend fun run(event: TestEvent) = Unit
}

object Second {
    @Action("dup.action")
    suspend fun run(event: TestEvent) = Unit
}
""",
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("重复声明"), result.messages)
    }

    @Test
    fun `rejects rules whose actionId is missing`() {
        val (_, result) = compile(
            """
package fixture

$RUNTIME_IMPORTS

object Rules {
    @Before(actionId = "missing.action")
    suspend fun check(ctx: ActionContext) = Unit
}
""",
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("不存在的 actionId"), result.messages)
    }

    @Test
    fun `rejects rule returning an unsupported type`() {
        val (_, result) = compile(
            """
package fixture

$RUNTIME_IMPORTS

class TestEvent

object Usecase {
    @Action("bad.rule")
    suspend fun run(event: TestEvent) = Unit
}

object Rules {
    @After(actionId = "bad.rule")
    suspend fun settle(ctx: ActionContext): String = "nope"
}
""",
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("只能返回 Unit 或 ActionResult"), result.messages)
    }

    @Test
    fun `generates facade without context injection for a single event action`() {
        val (compilation, result) = compile(
            """
package fixture

$RUNTIME_IMPORTS

class TestEvent

object PingUsecase {
    @Action("ping.run")
    suspend fun ping(event: TestEvent): Int = event.hashCode()
}
""",
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val facade = compilation.generatedSources().getValue("PingUsecaseActions.kt")
        assertTrue(facade.contains("ActionExecution<kotlin.Int>"), facade)
        assertTrue(facade.contains("PingUsecase.ping(event)"), facade)
        assertTrue(facade.contains("{ _ ->"), facade)
    }

    @Test
    fun `rejects unqualified parameter when multiple providers share the type`() {
        val (_, result) = compile(
            """
package fixture

$RUNTIME_IMPORTS

class TestEvent
data class Account(val id: Long)

object Usecase {
    @Action("account.run")
    suspend fun run(event: TestEvent) = Unit
}

object Providers {
    @Provider(qualifier = "from", actionId = "account.run")
    suspend fun from(ctx: ActionContext): Account = Account(1)

    @Provider(qualifier = "to", actionId = "account.run")
    suspend fun to(ctx: ActionContext): Account = Account(2)
}

object Rules {
    @Before(actionId = "account.run")
    suspend fun check(account: Account) = Unit
}
""",
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("请用 @FromProvider 指定限定名"), result.messages)
    }

    @Test
    fun `rejects provider returning Unit`() {
        val (_, result) = compile(
            """
package fixture

$RUNTIME_IMPORTS

object Providers {
    @Provider(qualifier = "nothing")
    suspend fun nothing(ctx: ActionContext) = Unit
}
""",
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(result.messages.contains("Provider 必须返回不可空的非 Unit 类类型"), result.messages)
    }

    private fun compile(source: String): Pair<KotlinCompilation, KotlinCompilation.Result> {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Fixture.kt", source))
            symbolProcessorProviders = listOf(ActionSymbolProcessorProvider())
            kspArgs = mutableMapOf("actionchain.moduleId" to "fixture")
            kspWithCompilation = true
            jvmTarget = "11"
            inheritClassPath = true
            messageOutputStream = System.out
        }
        return compilation to compilation.compile()
    }

    private fun KotlinCompilation.generatedSources(): Map<String, String> =
        kspSourcesDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associate { it.name to it.readText() }
}
