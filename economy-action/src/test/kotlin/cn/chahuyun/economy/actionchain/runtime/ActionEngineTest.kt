package cn.chahuyun.economy.actionchain.runtime

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class ActionEngineTest {
    private data class TestEvent(val userId: Long)
    private data class Account(val id: Long)

    @Test
    fun `runs before and after by order and exposes action result`(): Unit = runBlocking {
        val order = mutableListOf<String>()
        val engine = engine(
            module(
                actions = setOf("test.run"),
                rules = listOf(
                    ActionRule.before("test.run", "before.z", 20) {
                        order += "before-z"
                        ActionResult.Continue
                    },
                    ActionRule.before("test.run", "before.a", 20) {
                        order += "before-a"
                        ActionResult.Continue
                    },
                    ActionRule.after("test.run", "after", 5) {
                        order += "after:${it.actionResult(Int::class)}"
                        ActionResult.Continue
                    },
                ),
            ),
        )

        val execution = engine.invokeFromEntry("test.run", TestEvent(7)) { 42 }

        assertEquals(listOf("before-a", "before-z", "after:42"), order)
        assertTrue(execution.actionExecuted)
        assertEquals(42, execution.value)
        assertEquals(ActionResult.Continue, execution.result)
    }

    @Test
    fun `before terminal result skips action and after`(): Unit = runBlocking {
        var actionCalls = 0
        var afterCalls = 0
        val engine = engine(
            module(
                actions = setOf("test.stop"),
                rules = listOf(
                    ActionRule.before("test.stop", "stop", invoke = { ActionResult.EndSilently }),
                    ActionRule.after("test.stop", "after", invoke = {
                        afterCalls++
                        ActionResult.Continue
                    }),
                ),
            ),
        )

        val execution = engine.invokeFromEntry("test.stop", TestEvent(1)) {
            actionCalls++
        }

        assertFalse(execution.actionExecuted)
        assertNull(execution.value)
        assertEquals(ActionResult.EndSilently, execution.result)
        assertEquals(0, actionCalls)
        assertEquals(0, afterCalls)
    }

    @Test
    fun `message result is dispatched once and stops remaining after rules`(): Unit = runBlocking {
        val handled = mutableListOf<String>()
        val engine = ActionEngine(
            modules = listOf(
                module(
                    actions = setOf("test.reply"),
                    rules = listOf(
                        ActionRule.after("test.reply", "reply", 1) {
                            ActionResult.EndWithMessage("done")
                        },
                        ActionRule.after("test.reply", "later", 2) {
                            fail("after rule should have stopped")
                        },
                    ),
                ),
            ),
            resultHandler = ActionResultHandler { context, result ->
                handled += "${context.actionId}:${result.message}"
            },
        )

        val execution = engine.invokeFromEntry("test.reply", TestEvent(1)) { "value" }

        assertEquals(listOf("test.reply:done"), handled)
        assertEquals(ActionResult.EndWithMessage("done"), execution.result)
        assertEquals("value", execution.value)
    }

    @Test
    fun `action exception propagates without running after or reply handler`(): Unit = runBlocking {
        var afterCalls = 0
        var handlerCalls = 0
        val engine = ActionEngine(
            modules = listOf(
                module(
                    actions = setOf("test.fail"),
                    rules = listOf(ActionRule.after("test.fail", "after", invoke = {
                        afterCalls++
                        ActionResult.EndWithMessage("should not dispatch")
                    })),
                ),
            ),
            resultHandler = ActionResultHandler { _, _ -> handlerCalls++ },
        )

        try {
            engine.invokeFromEntry("test.fail", TestEvent(1)) { error("expected") }
            fail("exception should propagate")
        } catch (error: IllegalStateException) {
            assertEquals("expected", error.message)
        }

        assertEquals(0, afterCalls)
        assertEquals(0, handlerCalls)
    }

    @Test
    fun `provider qualifier separates same type and values are cached per invocation`(): Unit = runBlocking {
        var fromLoads = 0
        var toLoads = 0
        val engine = engine(
            module(
                actions = setOf("transfer.run"),
                providers = listOf(
                    actionProvider<Account>(qualifier = "fromUser", actionId = "transfer.run") {
                        fromLoads++
                        Account(it.event.let { event -> (event as TestEvent).userId })
                    },
                    actionProvider<Account>(qualifier = "toUser", actionId = "transfer.run") {
                        toLoads++
                        Account((it.event as TestEvent).userId + 1)
                    },
                ),
            ),
        )

        lateinit var from: Account
        lateinit var to: Account
        engine.invokeFromEntry("transfer.run", TestEvent(10)) { context ->
            from = context.provide(Account::class, "fromUser")
            assertSame(from, context.provide(Account::class, "fromUser"))
            to = context.provide(Account::class, "toUser")
        }

        assertEquals(Account(10), from)
        assertEquals(Account(11), to)
        assertEquals(1, fromLoads)
        assertEquals(1, toLoads)
    }

    @Test
    fun `global provider is visible to actions and cache does not cross invocations`(): Unit = runBlocking {
        var loads = 0
        val engine = engine(
            module(
                actions = setOf("account.one", "account.two"),
                providers = listOf(actionProvider<Account> {
                    loads++
                    Account(loads.toLong())
                }),
            ),
        )

        val first = engine.invokeFromEntry("account.one", TestEvent(1)) { context ->
            context.provide(Account::class)
        }
        val second = engine.invokeFromEntry("account.two", TestEvent(2)) { context ->
            context.provide(Account::class)
        }

        assertEquals(Account(1), first.value)
        assertEquals(Account(2), second.value)
        assertEquals(2, loads)
    }

    @Test
    fun `manual context values do not bypass provider loading`(): Unit = runBlocking {
        var loads = 0
        val engine = engine(
            module(
                actions = setOf("account.run"),
                providers = listOf(actionProvider<Account> {
                    loads++
                    Account(7)
                }),
            ),
        )

        val execution = engine.invokeFromEntry("account.run", TestEvent(1)) { context ->
            context.put(Account::class, Account(0))
            context.provide(Account::class)
        }

        assertEquals(Account(7), execution.value)
        assertEquals(1, loads)
    }

    @Test
    fun `unqualified provider resolves only a unique type and action local providers stay scoped`(): Unit = runBlocking {
        val engine = engine(
            module(
                actions = setOf("account.one", "account.two"),
                providers = listOf(
                    actionProvider<Account>(actionId = "account.one") { Account(1) },
                    actionProvider<Account>(qualifier = "other", actionId = "account.one") { Account(2) },
                ),
            ),
        )

        val ambiguity = runCatching {
            engine.invokeFromEntry("account.one", TestEvent(1)) { context ->
                context.provide(Account::class)
            }
        }.exceptionOrNull()
        assertIs<ActionConfigurationException>(ambiguity)

        val missing = runCatching {
            engine.invokeFromEntry("account.two", TestEvent(1)) { context ->
                context.provide(Account::class)
            }
        }.exceptionOrNull()
        assertIs<ActionConfigurationException>(missing)
    }

    @Test
    fun `rejects global and local provider collisions during engine construction`(): Unit {
        val error = runCatching {
            engine(
                module(
                    actions = setOf("account.run"),
                    providers = listOf(
                        actionProvider<Account>(qualifier = "user") { Account(1) },
                        actionProvider<Account>(qualifier = "user", actionId = "account.run") { Account(2) },
                    ),
                ),
            )
        }.exceptionOrNull()

        assertIs<ActionConfigurationException>(error)
    }

    @Test
    fun `rejects rules and providers referencing unknown actions`(): Unit {
        val ruleError = runCatching {
            engine(module(rules = listOf(ActionRule.before("missing", "rule", invoke = { ActionResult.Continue }))))
        }.exceptionOrNull()
        val providerError = runCatching {
            engine(
                module(providers = listOf(actionProvider<Account>(actionId = "missing") { Account(1) })),
            )
        }.exceptionOrNull()

        assertIs<ActionConfigurationException>(ruleError)
        assertIs<ActionConfigurationException>(providerError)
    }

    private fun module(
        actions: Set<String> = setOf("test.run"),
        rules: List<ActionRule> = emptyList(),
        providers: List<ActionProvider<out Any>> = emptyList(),
    ) = ActionModule("test-module", actions, rules, providers)

    private fun engine(module: ActionModule) = ActionEngine(
        modules = listOf(module),
        resultHandler = ActionResultHandler { _, _ -> Unit },
    )
}
