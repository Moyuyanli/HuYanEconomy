package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.EconomyCore
import cn.chahuyun.economy.base.ledger.BaseLedger
import cn.chahuyun.economy.base.ledger.LedgerBook
import cn.chahuyun.hibernateplus.DriveType
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.chahuyun.hibernateplus.HibernatePlusService
import org.junit.AfterClass
import org.junit.BeforeClass
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.*

/**
 * [EconomyProxy] 对接 **economy-base** 后端的 H2 集成测试。
 *
 * 2.0.1 的经济操作已全部改走本代理，因此这里锁定四类位置与两类资金语义：
 * 1. 钱包 / 主银行 / 插件私有账户 / 全局银行账户落在不同 scope，互不串账；
 * 2. `debit` 余额不足即失败，不会写出负余额；
 * 3. `adjust` 负数允许透支（债务转移场景唯一合法用法）；
 * 4. `transferTo` 在钱包与主银行之间原子搬运，余额不足时两端都不变；
 * 5. `Double` 重载按两位 HALF_UP 舍入。
 */
class EconomyProxyH2Test {

    companion object {
        private const val DB_PATH = "build/h2-it/economy-proxy-it"
        private const val ENTITY_PACKAGE = "cn.chahuyun.economy.base.entity"

        @JvmStatic
        @BeforeClass
        fun startDatabase() {
            val database = File(DB_PATH).absoluteFile
            database.parentFile.mkdirs()
            listOf(".mv.db", ".trace.db", ".lock.db").forEach { File(database.path + it).delete() }

            val configuration = HibernatePlusService.createConfiguration(EconomyCore::class.java).apply {
                driveType = DriveType.H2
                address = database.absolutePath.replace('\\', '/')
                packageName = ENTITY_PACKAGE
            }
            HibernatePlusService.loadingService(configuration)
            EconomyCore.init()
            BaseLedger.start()
            // 生产里由 EconomyLedgerProvider 提供后端；测试直接注入 base 后端。
            EconomyProxy.useBackend(BaseLedger)
        }

        @JvmStatic
        @AfterClass
        fun stopDatabase() {
            EconomyProxy.useBackend(null)
            runCatching { BaseLedger.stop() }
            runCatching { HibernateFactory.getSessionFactory().close() }
        }
    }

    private fun assertMoney(expected: String, actual: BigDecimal) {
        assertEquals(expected, actual.setScale(2, RoundingMode.HALF_UP).toPlainString())
    }

    private fun amount(value: String): BigDecimal = BigDecimal(value)

    // ---------------------------------------------------------------- 四类位置

    @Test
    fun `wallet and main bank are separate scopes`() {
        val qq = 92001L
        EconomyProxy.wallet(qq).credit(amount("100"))
        EconomyProxy.userBank(qq).credit(amount("40"))

        assertMoney("100.00", EconomyProxy.wallet(qq).balance())
        assertMoney("40.00", EconomyProxy.userBank(qq).balance())

        assertNotNull(BaseLedger.entries(LedgerBook.WALLET).firstOrNull { it.accountId == "u$qq" })
        assertNotNull(BaseLedger.entries(LedgerBook.USER_BANK).firstOrNull { it.accountId == "u$qq" })
    }

    @Test
    fun `plugin account keeps its own account id in the plugin book`() {
        val account = EconomyProxy.plugin("PB01-P", "pb-reserve")
        account.credit(amount("12.5"))

        assertMoney("12.50", account.balance())
        val entry = BaseLedger.entries(LedgerBook.PLUGIN).firstOrNull { it.accountId == "PB01-P" }
        assertNotNull(entry, "插件账户应落在 PLUGIN 账本")
        assertMoney("12.50", entry.amount)
    }

    @Test
    fun `global account is isolated from plugin account with the same id`() {
        EconomyProxy.global("POOL-1", "bank-pool").credit(amount("7"))
        EconomyProxy.plugin("POOL-1", "bank-pool").credit(amount("3"))

        assertMoney("7.00", EconomyProxy.global("POOL-1", "bank-pool").balance())
        assertMoney("3.00", EconomyProxy.plugin("POOL-1", "bank-pool").balance())
    }

    // ---------------------------------------------------------------- 资金语义

    @Test
    fun `debit fails when balance is insufficient and never goes negative`() {
        val qq = 92002L
        EconomyProxy.wallet(qq).credit(amount("10"))

        assertFalse(EconomyProxy.wallet(qq).debit(amount("11")).success)
        assertMoney("10.00", EconomyProxy.wallet(qq).balance())

        assertTrue(EconomyProxy.wallet(qq).debit(amount("4")).success)
        assertMoney("6.00", EconomyProxy.wallet(qq).balance())
    }

    @Test
    fun `adjust with negative amount overdrafts only for debt scenarios`() {
        val qq = 92003L
        EconomyProxy.wallet(qq).credit(amount("10"))

        val op = EconomyProxy.wallet(qq).adjust(amount("-30"))

        assertTrue(op.success, "adjust 负数分支允许透支: ${op.message}")
        assertMoney("-20.00", EconomyProxy.wallet(qq).balance())
    }

    @Test
    fun `transfer moves money atomically between wallet and bank`() {
        val qq = 92004L
        EconomyProxy.wallet(qq).credit(amount("100"))

        val op = EconomyProxy.wallet(qq).transferTo(EconomyProxy.userBank(qq), amount("40"))

        assertTrue(op.success, "转账应成功: ${op.message}")
        assertMoney("60.00", EconomyProxy.wallet(qq).balance())
        assertMoney("40.00", EconomyProxy.userBank(qq).balance())
    }

    @Test
    fun `failed transfer leaves both sides untouched`() {
        val qq = 92005L
        EconomyProxy.wallet(qq).credit(amount("5"))
        EconomyProxy.userBank(qq).credit(amount("1"))

        assertFalse(EconomyProxy.wallet(qq).transferTo(EconomyProxy.userBank(qq), amount("50")).success)
        assertMoney("5.00", EconomyProxy.wallet(qq).balance())
        assertMoney("1.00", EconomyProxy.userBank(qq).balance())
    }

    // ---------------------------------------------------------------- 精度与聚合

    @Test
    fun `double overloads round half up to two decimals`() {
        val qq = 92006L
        EconomyProxy.wallet(qq).credit(10.005)

        assertMoney("10.01", EconomyProxy.wallet(qq).balance())
    }

    @Test
    fun `bank total aggregates the user bank book`() {
        val before = EconomyProxy.bankTotal()
        EconomyProxy.userBank(92007L).credit(amount("25"))

        assertMoney((before + amount("25")).setScale(2, RoundingMode.HALF_UP).toPlainString(), EconomyProxy.bankTotal())
    }

    @Test
    fun `wallet by account id matches the qq encoded wallet`() {
        val qq = 92008L
        EconomyProxy.wallet(qq).credit(amount("9"))

        assertMoney("9.00", EconomyProxy.walletById(EconomyProxy.walletId(qq)).balance())
    }
}
