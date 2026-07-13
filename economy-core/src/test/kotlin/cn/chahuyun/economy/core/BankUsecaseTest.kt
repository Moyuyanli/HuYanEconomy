package cn.chahuyun.economy.core

import cn.chahuyun.economy.usecase.BankRoute
import cn.chahuyun.economy.usecase.BankTransferParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BankTransferParserTest {

    @Test
    fun `bank transfer parser accepts prefixed withdraw commands`() {
        assertTransfer("#取款 10M", 10_000_000.0, BankRoute.MAIN, null)
        assertTransfer("#取款 10m !", 10_000_000.0, BankRoute.MAIN, null)
        assertTransfer("#取款 20000 !", 20_000.0, BankRoute.MAIN, null)
        assertTransfer("#取款!!", null, BankRoute.MAIN, null)
        assertTransfer("#取款!", null, BankRoute.DEFAULT, null)
    }

    @Test
    fun `bank transfer parser accepts prefixed deposit commands`() {
        assertTransfer("#存款 52.3M", 52_300_000.0, BankRoute.MAIN, null, "存款", "deposit")
        assertTransfer("#存款 52.3M teafox", 52_300_000.0, BankRoute.PRIVATE, "teafox", "存款", "deposit")
        assertTransfer("/deposit 20K main", 20_000.0, BankRoute.MAIN, null, "存款", "deposit")
    }

    @Test
    fun `bank transfer parser rejects unrelated text`() {
        assertNull(BankTransferParser.parse("#银行列表", "取款", "withdraw"))
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
