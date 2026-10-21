package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.ledger.LedgerBook
import cn.chahuyun.economy.base.ledger.LedgerOwner
import kotlin.test.*

/**
 * mirai-economy 账户 id 编解码规则。
 *
 * 这层规则决定"同一笔钱在两套后端里是不是同一条记录"，写错会导致余额落进
 * 另一个账户或迁移漏读，因此逐条覆盖边界。
 */
class MiraiAccountIdTest {

    @Test
    fun `wallet keeps bare id so legacy upstream wallet stays compatible`() {
        assertEquals("u123456", MiraiAccountId.encode(LedgerOwner.Wallet("u123456")))
    }

    @Test
    fun `each scope gets its own suffix`() {
        assertEquals("u123456#BANK", MiraiAccountId.encode(LedgerOwner.UserBank("u123456")))
        assertEquals("prv_001#PLUGIN", MiraiAccountId.encode(LedgerOwner.PluginAccount("prv_001")))
        assertEquals("bank_pool#GLOBAL", MiraiAccountId.encode(LedgerOwner.GlobalAccount("bank_pool")))
    }

    @Test
    fun `same account id under different scopes never collides`() {
        val wallet = MiraiAccountId.encode(LedgerOwner.Wallet("u123456"))
        val bank = MiraiAccountId.encode(LedgerOwner.UserBank("u123456"))
        val plugin = MiraiAccountId.encode(LedgerOwner.PluginAccount("u123456"))
        assertEquals(3, setOf(wallet, bank, plugin).size, "三个 scope 必须编码成三个不同的上游账户")
    }

    @Test
    fun `decode round trips every scope`() {
        listOf(
            LedgerOwner.Wallet("u1"),
            LedgerOwner.UserBank("u1"),
            LedgerOwner.PluginAccount("p1"),
            LedgerOwner.GlobalAccount("g1"),
        ).forEach { owner ->
            val decoded = MiraiAccountId.decode(MiraiAccountId.encode(owner))
            assertEquals(MiraiAccountId.rawId(owner), decoded.accountId)
        }
    }

    @Test
    fun `decode splits on last separator so account name may contain hash`() {
        val decoded = MiraiAccountId.decode("prv#a#b#PLUGIN")
        assertEquals("prv#a#b", decoded.accountId)
        assertEquals(MiraiAccountId.PLUGIN, decoded.suffix)
    }

    @Test
    fun `unknown suffix is treated as plain account name`() {
        val decoded = MiraiAccountId.decode("foo#bar")
        assertEquals("foo#bar", decoded.accountId)
        assertNull(decoded.suffix)
    }

    @Test
    fun `trailing separator is not a suffix`() {
        val decoded = MiraiAccountId.decode("u123456#")
        assertEquals("u123456#", decoded.accountId)
        assertNull(decoded.suffix)
    }

    @Test
    fun `plugin account named like a user is not classified as wallet`() {
        val encoded = MiraiAccountId.encode(LedgerOwner.PluginAccount("u999"))
        assertEquals("u999#PLUGIN", encoded)

        val decoded = MiraiAccountId.decode(encoded)
        assertFalse(MiraiAccountId.belongsTo(decoded, LedgerBook.WALLET))
        assertTrue(MiraiAccountId.belongsTo(decoded, LedgerBook.PLUGIN))
    }

    @Test
    fun `classification covers all books`() {
        val cases = mapOf(
            "u123456" to LedgerBook.WALLET,
            "u123456#BANK" to LedgerBook.USER_BANK,
            "prv_001#PLUGIN" to LedgerBook.PLUGIN,
            "bank_pool#GLOBAL" to LedgerBook.GLOBAL,
        )
        cases.forEach { (rawId, expected) ->
            val decoded = MiraiAccountId.decode(rawId)
            LedgerBook.values().forEach { book ->
                assertEquals(
                    book == expected,
                    MiraiAccountId.belongsTo(decoded, book),
                    "$rawId 应只属于 $expected，实际判定 $book 时结果不符",
                )
            }
        }
    }

    @Test
    fun `bare non user account is not a wallet`() {
        val decoded = MiraiAccountId.decode("bank_pool")
        assertFalse(MiraiAccountId.belongsTo(decoded, LedgerBook.WALLET))
        assertFalse(MiraiAccountId.belongsTo(decoded, LedgerBook.GLOBAL))
    }

    @Test
    fun `blank account id is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            MiraiAccountId.encode(LedgerOwner.PluginAccount("   "))
        }
    }

    @Test
    fun `over long encoded id is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            MiraiAccountId.encode(LedgerOwner.PluginAccount("a".repeat(200)))
        }
    }

    @Test
    fun `account id is trimmed`() {
        assertEquals("prv_001#PLUGIN", MiraiAccountId.encode(LedgerOwner.PluginAccount("  prv_001  ")))
    }
}
