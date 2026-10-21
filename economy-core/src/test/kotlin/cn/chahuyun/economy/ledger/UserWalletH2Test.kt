package cn.chahuyun.economy.ledger

import cn.chahuyun.economy.base.EconomyAccount
import cn.chahuyun.economy.base.EconomyCore
import cn.chahuyun.economy.base.ledger.BaseLedger
import cn.chahuyun.economy.base.ledger.LedgerBook
import cn.chahuyun.economy.base.ledger.LedgerOwner
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
 * [UserWallet] 对接 **economy-base** 后端的 H2 集成测试。
 *
 * 这是签到新经济接入的第一道验证：签到只调用 `credit` / `balance` 两个方法，
 * 只要这两个方法在真实数据库上行为正确，"签到发钱"这条链就是通的。
 *
 * 覆盖点：
 * 1. 入账真的写进 `economy_account` 的 `DEFAULT` scope（账户标识 `u<qq>`）；
 * 2. 多次入账累加，且返回的余额就是入账后的值（签到回显依赖它）；
 * 3. `Double` 金额按两位 HALF_UP 落库，不出现浮点尾巴；
 * 4. 钱包与主银行是两个 scope，互不影响；
 * 5. 非正数金额被拒绝，余额保持不变；
 * 6. 未入账过的账户读出来是 0，且能被 `WALLET` 账本枚举到（迁移/排行依赖枚举）。
 *
 * 说明：hibernate-plus 的 H2 驱动固定拼成 `jdbc:h2:file:<address>`，所以用文件库，
 * 并在启动时删掉旧文件保证每次运行都是干净状态。
 */
class UserWalletH2Test {

    companion object {
        private const val DB_PATH = "build/h2-it/user-wallet-it"
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
            // 显式启动 base 后端：生产里由 EconomyLedgerProvider 调用，测试里直接注入。
            BaseLedger.start()
        }

        @JvmStatic
        @AfterClass
        fun stopDatabase() {
            runCatching { BaseLedger.stop() }
            runCatching { HibernateFactory.getSessionFactory().close() }
        }
    }

    /** 被测对象：把 base 后端注入进去，避免依赖全局 Provider 的启动状态。 */
    private val wallet = UserWallet(BaseLedger)

    /** 统一按两位小数比较，避免 BigDecimal.equals 因 scale 差异误判。 */
    private fun assertMoney(expected: String, actual: BigDecimal) {
        assertEquals(expected, actual.setScale(2, RoundingMode.HALF_UP).toPlainString())
    }

    @Test
    fun `backend is economy-base`() {
        assertEquals(BaseLedger.backendId, wallet.backendId)
    }

    // ---------------------------------------------------------------- 入账

    @Test
    fun `credit writes the reward into the default scope wallet`() {
        val qq = 90001L
        val op = wallet.credit(qq, BigDecimal("100.00"), "签到测试")

        assertTrue(op.success, "入账应成功: ${op.message}")
        assertMoney("100.00", assertNotNull(op.balance))
        // 账户落在 DEFAULT scope、账户名就是 u<qq>，与 UserInfoDto.id 同构。
        assertEquals("100.00", EconomyAccount.take("u$qq").formatBalance())
        assertMoney("100.00", wallet.balance(qq))
    }

    @Test
    fun `repeated credits accumulate`() {
        val qq = 90002L
        wallet.credit(qq, UserWallet.money(50.0))
        wallet.credit(qq, UserWallet.money(120.0))

        assertMoney("170.00", wallet.balance(qq))
    }

    @Test
    fun `credit returns the balance after the operation`() {
        val qq = 90003L
        wallet.credit(qq, UserWallet.money(30.0))
        val second = wallet.credit(qq, UserWallet.money(45.0), "签到测试")

        assertTrue(second.success)
        // 签到回显直接复用这个快照，必须等于最新余额。
        assertMoney("75.00", assertNotNull(second.balance))
    }

    // ---------------------------------------------------------------- 精度

    @Test
    fun `double amounts are rounded half up to two decimals`() {
        // 二进制浮点下 10.005 实际略小于 10.005，valueOf + HALF_UP 得到 10.01；
        // 直接用 BigDecimal(10.005) 会得到 10.00 附近的值，这条用例就是防这个。
        assertEquals("10.01", UserWallet.money(10.005).toPlainString())

        val qq = 90004L
        wallet.credit(qq, UserWallet.money(10.005))
        assertMoney("10.01", wallet.balance(qq))
    }

    // ---------------------------------------------------------------- 隔离

    @Test
    fun `wallet is isolated from the user main bank`() {
        val qq = 90005L
        wallet.credit(qq, UserWallet.money(88.0))

        assertMoney("88.00", wallet.balance(qq))
        // 同一个 qq 的主银行是另一个 scope，不应被钱包入账影响。
        assertMoney("0.00", BaseLedger.balance(LedgerOwner.UserBank("u$qq")))
    }

    @Test
    fun `unknown account reads as zero`() {
        assertMoney("0.00", wallet.balance(90006L))
    }

    // ---------------------------------------------------------------- 拒绝

    @Test
    fun `non positive amounts are rejected without touching the balance`() {
        val qq = 90007L
        wallet.credit(qq, UserWallet.money(20.0))

        assertFalse(wallet.credit(qq, UserWallet.money(0.0)).success, "0 金额应被拒绝")
        assertFalse(wallet.credit(qq, UserWallet.money(-5.0)).success, "负数金额应被拒绝")
        assertMoney("20.00", wallet.balance(qq))
    }

    @Test
    fun `debit fails when the wallet is insufficient`() {
        val qq = 90008L
        wallet.credit(qq, UserWallet.money(10.0))

        assertFalse(wallet.debit(qq, UserWallet.money(11.0)).success)
        assertMoney("10.00", wallet.balance(qq))

        assertTrue(wallet.debit(qq, UserWallet.money(4.0)).success)
        assertMoney("6.00", wallet.balance(qq))
    }

    // ---------------------------------------------------------------- 账本视图

    @Test
    fun `credited accounts show up in the wallet book`() {
        val qq = 90009L
        wallet.credit(qq, UserWallet.money(66.0))

        val entry = BaseLedger.entries(LedgerBook.WALLET).firstOrNull { it.accountId == "u$qq" }
        assertNotNull(entry, "钱包账本应能枚举到 u$qq")
        assertMoney("66.00", entry.amount)
    }
}
