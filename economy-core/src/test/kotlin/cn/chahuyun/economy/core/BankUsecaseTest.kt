package cn.chahuyun.economy.core

import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.usecase.BankRoute
import cn.chahuyun.economy.usecase.BankTransferParser
import cn.chahuyun.economy.usecase.BankUsecase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BankUsecaseTest {

    @Test
    fun `regal top aggregates account id funding and qq accounts for same user`() {
        val users = listOf(
            UserInfoDto(id = "u10001", qq = 10001, name = "Fox", registerGroup = 1, funding = "fund-fox"),
            UserInfoDto(id = "u10002", qq = 10002, name = "Tea", registerGroup = 1, funding = "fund-tea")
        )
        val accountBalances = mapOf(
            "u10001" to 100.0,
            "fund-fox" to 50.0,
            "10001" to 25.0,
            "u10002" to 120.0,
            "private-bank-ledger" to 10_000.0
        )

        val entries = BankUsecase.buildRegalTopEntries(accountBalances, users)

        assertEquals(listOf(10001L, 10002L), entries.map { it.userInfo.qq })
        assertEquals(175.0, entries[0].money)
        assertEquals(120.0, entries[1].money)
    }

    @Test
    fun `regal top snapshot keeps total money before limiting entries`() {
        val users = (1L..12L).map {
            UserInfoDto(id = "u$it", qq = it, name = "u$it", registerGroup = 1)
        }
        val accountBalances = (1L..12L).associate { "u$it" to it.toDouble() }

        val snapshot = BankUsecase.buildRegalTopSnapshot(accountBalances, users, limit = 10)

        assertEquals(10, snapshot.entries.size)
        assertEquals((1L..12L).sumOf { it.toDouble() }, snapshot.totalMoney)
        assertEquals(12.0, snapshot.entries.first().money)
        assertEquals(3.0, snapshot.entries.last().money)
    }
}

class BankTransferParserTest {

    @Test
    fun `bank transfer parser accepts prefixed withdraw commands`() {
        assertTransfer("#取款 10M", 10_000_000.0, BankRoute.DEFAULT, null)
        assertTransfer("#取款 10m !", 10_000_000.0, BankRoute.MAIN, null)
        assertTransfer("#取款 20000 !", 20_000.0, BankRoute.MAIN, null)
        assertTransfer("#取款!!", null, BankRoute.MAIN, null)
        assertTransfer("#取款!", null, BankRoute.DEFAULT, null)
    }

    @Test
    fun `bank transfer parser accepts prefixed deposit commands`() {
        assertTransfer("#存款 52.3M", 52_300_000.0, BankRoute.DEFAULT, null, "存款", "deposit")
        assertTransfer("#存款 52.3M teafox", 52_300_000.0, BankRoute.PRIVATE, "teafox", "存款", "deposit")
        assertTransfer("/deposit 20K main", 20_000.0, BankRoute.MAIN, null, "存款", "deposit")
    }

    @Test
    fun `bank transfer parser rejects unrelated text`() {
        assertNull(BankTransferParser.parse("#银行列表", "取款", "withdraw"))
    }

    @Test
    fun `explicit main always routes to main bank`() {
        assertTransfer("取款 10M main", 10_000_000.0, BankRoute.MAIN, null)
        assertTransfer("存款 10M 主银行", 10_000_000.0, BankRoute.MAIN, null, "存款", "deposit")
    }

    @Test
    fun `blank and legacy main defaults resolve to main bank`() {
        assertNull(BankUsecase.defaultPrivateBankKey(""))
        assertNull(BankUsecase.defaultPrivateBankKey("main"))
        assertNull(BankUsecase.defaultPrivateBankKey("主银行"))
        assertEquals("teafox", BankUsecase.defaultPrivateBankKey(" teafox "))
    }

    private fun assertTransfer(
        raw: String,
        amount: Double?,
        route: BankRoute,
        bankKey: String?,
        chineseCommand: String = "取款",
        englishCommand: String = "withdraw"
    ) {
        val request = assertNotNull(BankTransferParser.parse(raw, chineseCommand, englishCommand))
        assertEquals(amount, request.amount)
        assertEquals(route, request.route)
        assertEquals(bankKey, request.bankKey)
    }
}
