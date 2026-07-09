package cn.chahuyun.economy.image

import cn.chahuyun.economy.image.model.*
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

    @Test
    fun `farm detail image renders as non blank canvas`() {
        val plots = (1..18).map { index ->
            when {
                index <= 3 -> FarmPlotDetailLine(
                    plotNo = index,
                    title = "🥕 胡萝卜",
                    subtitle = "第1/2季",
                    statusText = if (index == 1) "可收获" else "成长中",
                    progressText = if (index == 1) "已成熟" else "${index * 8}分",
                    status = if (index == 1) FarmPlotDetailStatus.READY else FarmPlotDetailStatus.GROWING,
                )

                index <= 8 -> FarmPlotDetailLine(
                    plotNo = index,
                    title = "空闲土地",
                    subtitle = "可播种",
                    statusText = "空闲",
                    progressText = "等待种植",
                    status = FarmPlotDetailStatus.EMPTY,
                )

                else -> FarmPlotDetailLine(
                    plotNo = index,
                    title = "未开拓",
                    subtitle = "升级农场解锁",
                    statusText = "锁定",
                    progressText = "待开拓",
                    status = FarmPlotDetailStatus.LOCKED,
                )
            }
        }

        val image = FarmDetailImageRenderer.render(
            FarmDetailCard(
                owner = "572490972",
                level = 4,
                unlockedPlots = 9,
                totalPlots = 18,
                plantedPlots = 3,
                readyPlots = 1,
                emptyPlots = 5,
                shieldText = "未激活",
                waterText = "13级开放",
                waterHint = "今日帮浇水次数每日自动重置",
                plots = plots,
            )
        )

        assertEquals(1280, image.width)
        assertEquals(960, image.height)
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
