package cn.chahuyun.economy.ledger

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 经济后端的类加载隔离约束。
 *
 * 需求是"mirai-economy 可选"：上游插件未安装时，插件必须能正常启动。只要
 * `EconomyUtil` / `BaseLedger` / `EconomyLedgerProvider` 的常量池里出现上游类型，
 * 这些类在未装上游的环境里就会抛 `NoClassDefFoundError`，所以这里直接扫描 class
 * 文件常量池做静态把关，防止以后有人在门面里顺手 import 上游类型。
 */
class LedgerIsolationTest {

    private val upstreamMarker = "xyz/cssxsh/mirai/economy"

    private fun containsUpstream(resource: String): Boolean {
        val stream = javaClass.classLoader.getResourceAsStream(resource)
            ?: error("找不到 class 资源: $resource")
        return stream.use { String(it.readBytes(), Charsets.ISO_8859_1).contains(upstreamMarker) }
    }

    @Test
    fun `economic facade and default backend must not reference upstream plugin`() {
        listOf(
            "cn/chahuyun/economy/utils/EconomyUtil.class",
            "cn/chahuyun/economy/base/ledger/BaseLedger.class",
            "cn/chahuyun/economy/base/ledger/EconomyLedger.class",
            "cn/chahuyun/economy/ledger/EconomyLedgerProvider.class",
            "cn/chahuyun/economy/ledger/MiraiEconomyProbe.class",
        ).forEach { resource ->
            assertFalse(containsUpstream(resource), "$resource 不应引用 $upstreamMarker")
        }
    }

    @Test
    fun `upstream types are isolated in the legacy api`() {
        assertTrue(
            containsUpstream("cn/chahuyun/economy/ledger/LegacyEconomyApi.class"),
            "LegacyEconomyApi 是上游实现的唯一载体，应包含上游引用",
        )
    }

    /**
     * 最关键的一条：core 的 testRuntime 里没有 mirai-economy-core（compileOnly 不会进 test runtime），
     * 所以能在这种环境里把 [MiraiEconomyLedger] 加载起来，就说明它的字段与构造签名里确实没有上游类型。
     */
    @Test
    fun `mirai ledger class can be loaded without upstream plugin on classpath`() {
        val upstreamMissing = runCatching {
            Class.forName("xyz.cssxsh.mirai.economy.EconomyService", false, javaClass.classLoader)
        }.isFailure
        assertTrue(upstreamMissing, "测试环境不应包含上游插件，否则该用例失去意义")

        val ledger = Class.forName("cn.chahuyun.economy.ledger.MiraiEconomyLedger", false, javaClass.classLoader)
        assertFalse(ledger.isInterface, "MiraiEconomyLedger 应可加载且是具体类")
    }
}
