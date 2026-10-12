package cn.chahuyun.economy.base

import cn.chahuyun.hibernateplus.DriveType
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.chahuyun.hibernateplus.HibernatePlusService
import org.junit.AfterClass
import org.junit.BeforeClass
import java.io.File
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * EconomyCore / EconomyAccount / EconomyAction 的 H2 集成测试。
 *
 * 直接用工程内置的 hibernate-plus 起一个真实的 H2 文件库，覆盖纯值类型测试覆盖不到的
 * 场景：账户按需创建、扣款、转账、余额越界保护、Scope 生命周期。
 *
 * 说明：hibernate-plus 的 H2 驱动固定拼成 `jdbc:h2:file:<address>`，所以这里用文件库而不是
 * 内存库，并在启动时先删掉旧库文件以保证每次运行都是干净状态。
 */
class EconomyCoreH2IntegrationTest {

    companion object {
        private const val DB_PATH = "build/h2-it/economy-base-it"
        private const val ENTITY_PACKAGE = "cn.chahuyun.economy.base.entity"

        @JvmStatic
        @BeforeClass
        fun startDatabase() {
            val database = File(DB_PATH).absoluteFile
            database.parentFile.mkdirs()
            // 每次运行都重建库文件，保证用例之间互不干扰。
            listOf(".mv.db", ".trace.db", ".lock.db").forEach { File(database.path + it).delete() }

            val configuration = HibernatePlusService.createConfiguration(EconomyCore::class.java).apply {
                driveType = DriveType.H2
                address = database.absolutePath.replace('\\', '/')
                // 只扫描 economy-base 自己的实体，避免把测试类当成实体。
                packageName = ENTITY_PACKAGE
            }
            HibernatePlusService.loadingService(configuration)
            EconomyCore.init()
        }

        @JvmStatic
        @AfterClass
        fun stopDatabase() {
            runCatching { HibernateFactory.getSessionFactory().close() }
        }
    }

    private fun account(name: String): EconomyAccount = EconomyAccount.take(name)

    private fun balanceOf(name: String): String = EconomyAccount.take(name).formatBalance()

    // ---------------------------------------------------------------- Scope

    @Test
    fun `init creates the reserved default scope`() {
        val scope = EconomyAccount.scope("DEFAULT")
        assertNotNull(scope)
        assertEquals("DEFAULT", scope.code)
        assertEquals("默认", scope.name)
        // 查询时的 code 会被标准化为大写。
        assertEquals(scope.id, EconomyAccount.scope("default")?.id)
    }

    @Test
    fun `reading an unknown scope returns null`() {
        assertNull(EconomyAccount.scope("NOT_A_REAL_SCOPE"))
        assertNull(EconomyAccount.scopeName("不存在的域"))
    }

    @Test
    fun `scope lifecycle create normalise and delete`() {
        val scope = EconomyCore.scopeCreate("集成测试域", "ith2")
        assertEquals("ITH2", scope.code)
        assertEquals("集成测试域", scope.name)
        assertNotNull(EconomyAccount.scope("ITH2"))

        // 重复的 code 必须被拒绝。
        assertFailsWith<IllegalArgumentException> { EconomyCore.scopeCreate("重复", "ITH2") }

        // 没有被账户引用时可以删除。
        assertTrue(EconomyCore.scopeDelete(scope))
        assertNull(EconomyAccount.scope("ITH2"))
    }

    @Test
    fun `default scope cannot be deleted`() {
        val scope = assertNotNull(EconomyAccount.scope("DEFAULT"))
        assertFailsWith<IllegalArgumentException> { EconomyCore.scopeDelete(scope) }
    }

    @Test
    fun `deleting a scope that still owns accounts returns false`() {
        val scope = EconomyCore.scopeCreate("被引用域", "ITH3")
        val scoped = EconomyCore.take("u30001", scope)
        assertEquals(scope.id, scoped.scope.id)

        assertFalse(EconomyCore.scopeDelete(scope))
        assertNotNull(EconomyAccount.take("u30001", scope))
    }

    // -------------------------------------------------------------- Accounts

    @Test
    fun `take creates a zero balance account on demand`() {
        val account = account("u20001")
        assertTrue(account.id > 0)
        assertEquals("u20001", account.account)
        assertEquals(EconomyAccount.DEFAULT_SCOPE, account.scope.code)
        assertEquals("0.00", account.formatBalance())
    }

    @Test
    fun `group and per group user accounts use the documented codes`() {
        assertEquals("g88001", EconomyAccount.group(88001).account)
        assertEquals("u88001g88002", EconomyAccount.userInGroup(88001, 88002).account)
    }

    @Test
    fun `blank account identifiers are rejected`() {
        val scope = assertNotNull(EconomyAccount.scope("DEFAULT"))
        assertFailsWith<IllegalArgumentException> { EconomyCore.take("   ", scope) }
    }

    // ---------------------------------------------------------------- Actions

    @Test
    fun `add persists the new balance`() {
        val result = EconomyAction.add(account("u20002"), 100)
        assertTrue(result.success)
        assertEquals("100.00", assertNotNull(result.balance).format())
        assertEquals("100.00", balanceOf("u20002"))
    }

    @Test
    fun `amounts are rounded to two decimals on the way in`() {
        EconomyAction.add(account("u20003"), BigDecimal("10.005"))
        assertEquals("10.01", balanceOf("u20003"))
    }

    @Test
    fun `subtract reduces the balance when it is sufficient`() {
        EconomyAction.add(account("u20004"), 50)
        val result = EconomyAction.subtract(account("u20004"), 20)
        assertTrue(result.success)
        assertEquals("30.00", assertNotNull(result.balance).format())
        assertEquals("30.00", balanceOf("u20004"))
    }

    @Test
    fun `subtract beyond the balance is rejected and keeps the stored value`() {
        EconomyAction.add(account("u20005"), 10)
        val result = EconomyAction.subtract(account("u20005"), 11)
        assertEquals(EconomyActionStatus.INSUFFICIENT_BALANCE, result.status)
        assertEquals("10.00", balanceOf("u20005"))
    }

    @Test
    fun `non positive amounts are rejected as invalid`() {
        EconomyAction.add(account("u20006"), 100)
        assertEquals(EconomyActionStatus.INVALID_AMOUNT, EconomyAction.add(account("u20006"), 0).status)
        assertEquals(EconomyActionStatus.INVALID_AMOUNT, EconomyAction.subtract(account("u20006"), -5).status)
        assertEquals("100.00", balanceOf("u20006"))
    }

    @Test
    fun `amounts above the decimal 19 2 upper bound are rejected`() {
        val tooBig = BigDecimal("100000000000000000.00")
        assertEquals(EconomyActionStatus.INVALID_AMOUNT, EconomyAction.add(account("u20007"), tooBig).status)
        assertEquals("0.00", balanceOf("u20007"))
    }

    @Test
    fun `a balance that would overflow the column is rejected without corrupting state`() {
        EconomyAction.add(account("u20008"), EconomyBalance.MAX_ABS)
        assertEquals("99999999999999999.99", balanceOf("u20008"))

        // 再加 1 会让余额越过 DECIMAL(19,2) 上限，必须在写入前被拒绝。
        assertEquals(EconomyActionStatus.INVALID_AMOUNT, EconomyAction.add(account("u20008"), 1).status)
        assertEquals("99999999999999999.99", balanceOf("u20008"))
    }

    // ---------------------------------------------------------------- Transfer

    @Test
    fun `transfer moves the balance between two accounts`() {
        EconomyAction.add(account("u20009"), 500)
        val result = EconomyAction.transfer(account("u20009"), account("u20010"), EconomyBalance.of(120))
        assertTrue(result.success)
        assertEquals("380.00", balanceOf("u20009"))
        assertEquals("120.00", balanceOf("u20010"))
    }

    @Test
    fun `transfer to the same account is rejected`() {
        val same = account("u20011")
        EconomyAction.add(same, 10)
        val result = EconomyAction.transfer(same, same, EconomyBalance.of(1))
        assertEquals(EconomyActionStatus.SAME_ACCOUNT, result.status)
        assertEquals("10.00", balanceOf("u20011"))
    }

    @Test
    fun `transfer with insufficient balance is rejected`() {
        EconomyAction.add(account("u20012"), 10)
        val result = EconomyAction.transfer(account("u20012"), account("u20013"), EconomyBalance.of(11))
        assertEquals(EconomyActionStatus.INSUFFICIENT_BALANCE, result.status)
        assertEquals("10.00", balanceOf("u20012"))
        assertEquals("0.00", balanceOf("u20013"))
    }

    @Test
    fun `transfer with a non positive amount is rejected`() {
        EconomyAction.add(account("u20014"), 10)
        val result = EconomyAction.transfer(account("u20014"), account("u20015"), EconomyBalance.of(0))
        assertEquals(EconomyActionStatus.INVALID_AMOUNT, result.status)
        assertEquals("10.00", balanceOf("u20014"))
    }

    @Test
    fun `transfer that would overflow the target account is rejected`() {
        EconomyAction.add(account("u20016"), EconomyBalance.MAX_ABS)
        EconomyAction.add(account("u20017"), 100)

        val result = EconomyAction.transfer(account("u20017"), account("u20016"), EconomyBalance.of(1))
        assertEquals(EconomyActionStatus.INVALID_AMOUNT, result.status)
        // 越界被挡在写入之前，两边余额都保持原值。
        assertEquals("100.00", balanceOf("u20017"))
        assertEquals("99999999999999999.99", balanceOf("u20016"))
    }
}
