package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.EconomyAccount
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
 * [EconomyProxy] 的**钱包路径**在 economy-base 后端上的 H2 集成测试。
 *
 * 签到这条链只用到 `wallet(qq).credit` / `wallet(qq).balance`，本类把这两个方法与
 * 账户编码、金额精度一起钉在真实数据库上；四类位置的隔离、转账、透支等语义由
 * [EconomyProxyH2Test] 覆盖，这里不重复。
 *
 * 覆盖点：
 * 1. 后端标识是 economy-base（注入生效）；
 * 2. 入账真的写进 `economy_account` 的 `DEFAULT` scope，账户标识 `u<qq>`；
 * 3. 多次入账累加，且返回的余额是入账后的快照（签到回显直接用它）；
 * 4. `Double` 金额按两位 HALF_UP 落库，不出现浮点尾巴；
 * 5. 未入账过的账户读出来是 0；
 * 6. 非正数金额被拒绝，余额不变；
 * 7. 入账账户能被 `WALLET` 账本枚举到（迁移/排行依赖枚举）。
 *
 * 说明：hibernate-plus 的 H2 驱动固定拼成 `jdbc:h2:file:<address>`，所以用文件库，
 * 并在启动时删掉旧文件保证每次运行都是干净状态。
 */
class EconomyProxyWalletH2Test {

    companion object {
        private const val DB_PATH = "build/h2-it/economy-proxy-wallet-it"
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
                // 只扫描 economy-base 的实体，避免把测试类当成实体。
                packageName = ENTITY_PACKAGE
            }
            HibernatePlusService.loadingService(configuration)
            EconomyCore.init()
            // 生产里由 EconomyLedgerProvider 按配置选择；测试直接注入本地账本。
            BaseLedger.start()
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

    /** 统一按两位小数比较，避免 BigDecimal.equals 因 scale 差异误判。 */
    private fun assertMoney(expected: String, actual: BigDecimal) {
        assertEquals(expected, actual.setScale(2, RoundingMode.HALF_UP).toPlainString())
    }

    @Test
    fun `injected backend is economy-base`() {
        assertEquals(BaseLedger.backendId, EconomyProxy.backendId)
    }

    // ---------------------------------------------------------------- 入账

    @Test
    fun `credit writes the reward into the default scope wallet`() {
        val qq = 91001L
        val op = EconomyProxy.wallet(qq).credit(EconomyProxy.money(100.0))

        assertTrue(op.success, "入账应成功: ${op.message}")
        assertMoney("100.00", assertNotNull(op.balance))
        // 账户落在 DEFAULT scope、账户名就是 u<qq>，与 UserInfoDto.id 同构。
        assertEquals("100.00", EconomyAccount.take(EconomyProxy.walletId(qq)).formatBalance())
        assertMoney("100.00", EconomyProxy.wallet(qq).balance())
    }

    @Test
    fun `repeated credits accumulate`() {
        val qq = 91002L
        EconomyProxy.wallet(qq).credit(50.0)
        EconomyProxy.wallet(qq).credit(120.0)

        assertMoney("170.00", EconomyProxy.wallet(qq).balance())
    }

    @Test
    fun `credit returns the balance after the operation`() {
        val qq = 91003L
        EconomyProxy.wallet(qq).credit(30.0)
        val second = EconomyProxy.wallet(qq).credit(EconomyProxy.money(45.0))

        assertTrue(second.success)
        // 签到回显直接复用这个快照，必须等于最新余额。
        assertMoney("75.00", assertNotNull(second.balance))
    }

    // ---------------------------------------------------------------- 精度

    @Test
    fun `double amounts are rounded half up to two decimals`() {
        // 二进制浮点下 10.005 实际略小于 10.005，valueOf + HALF_UP 得到 10.01；
        // 直接用 BigDecimal(10.005) 会得到 10.00 附近的值，这条用例就是防这个。
        assertEquals("10.01", EconomyProxy.money(10.005).toPlainString())

        val qq = 91004L
        EconomyProxy.wallet(qq).credit(10.005)
        assertMoney("10.01", EconomyProxy.wallet(qq).balance())
    }

    // ---------------------------------------------------------------- 边界

    @Test
    fun `unknown account reads as zero`() {
        assertMoney("0.00", EconomyProxy.wallet(91005L).balance())
    }

    @Test
    fun `non positive amounts are rejected without touching the balance`() {
        val qq = 91006L
        EconomyProxy.wallet(qq).credit(20.0)

        assertFalse(EconomyProxy.wallet(qq).credit(0.0).success, "0 金额应被拒绝")
        assertFalse(EconomyProxy.wallet(qq).credit(-5.0).success, "负数金额应被拒绝")
        assertMoney("20.00", EconomyProxy.wallet(qq).balance())
    }

    // ---------------------------------------------------------------- 账本视图

    @Test
    fun `credited accounts show up in the wallet book`() {
        val qq = 91007L
        EconomyProxy.wallet(qq).credit(66.0)

        val entry = BaseLedger.entries(LedgerBook.WALLET)
            .firstOrNull { it.accountId == EconomyProxy.walletId(qq) }
        assertNotNull(entry, "钱包账本应能枚举到 u$qq")
        assertMoney("66.00", entry.amount)
    }
}
