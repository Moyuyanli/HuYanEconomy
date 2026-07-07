package cn.chahuyun.economy.image

import cn.chahuyun.economy.image.model.BankInfoFundLine
import cn.chahuyun.economy.image.model.BankInfoLoanLine
import cn.chahuyun.economy.image.model.PrivateBankInfoCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EconomyImageRendererSmokeTest {

    @Test
    fun `main help image renders as non blank canvas`() {
        val image = EconomyImageRenderer.renderMainHelp()

        assertEquals(1280, image.width)
        assertEquals(1280, image.height)
        assertNonBlank(image)
    }

    @Test
    fun `game help image renders as non blank canvas`() {
        val image = EconomyImageRenderer.renderGameHelp()

        assertEquals(1280, image.width)
        assertEquals(1280, image.height)
        assertNonBlank(image)
    }

    @Test
    fun `personal info preview renders as non blank canvas`() {
        val image = EconomyImageRenderer.previewPersonalInfo()

        assertEquals(EconomyImageRenderer.PERSONAL_WIDTH, image.width)
        assertEquals(EconomyImageRenderer.PERSONAL_HEIGHT, image.height)
        assertNonBlank(image)
    }

    @Test
    fun `private bank info image renders as non blank canvas`() {
        val image = EconomyImageRenderer.renderPrivateBankInfo(
            PrivateBankInfoCard(
                name = "壶言中央银行",
                code = "huyan",
                slogan = "稳健经营，轻松娱乐",
                owner = "572490972",
                star = 4,
                interest = "3.5‰",
                avgReview = "4.8",
                totalDeposit = "88.6w",
                withdrawSuccessRate = "96%",
                defaulterUntil = "无",
                fundLines = listOf(
                    BankInfoFundLine("储户本金", "88.6w", "用户存款总额"),
                    BankInfoFundLine("准备金", "12.4w", "可用于兑付"),
                    BankInfoFundLine("狐卷资金", "6.8w", "竞标锁定")
                ),
                loanLines = listOf(
                    BankInfoLoanLine("可贷额度", "20.0w", "当前可放贷余额"),
                    BankInfoLoanLine("逾期贷款", "0", "暂无风险"),
                    BankInfoLoanLine("信用评级", "A", "自动评级")
                )
            )
        )

        assertEquals(1280, image.width)
        assertEquals(720, image.height)
        assertNonBlank(image)
    }

    private fun assertNonBlank(image: java.awt.image.BufferedImage) {
        val sampleStepX = (image.width / 16).coerceAtLeast(1)
        val sampleStepY = (image.height / 16).coerceAtLeast(1)
        val colors = mutableSetOf<Int>()

        for (y in 0 until image.height step sampleStepY) {
            for (x in 0 until image.width step sampleStepX) {
                colors += image.getRGB(x, y)
            }
        }

        assertTrue(colors.size > 4, "rendered image should contain multiple sampled colors")
        assertTrue(colors.any { (it ushr 24) != 0 }, "rendered image should contain visible pixels")
    }
}
