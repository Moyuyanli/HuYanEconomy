package cn.chahuyun.economy.utils

import cn.chahuyun.economy.EconomyBuildConstants
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * 帮助图里的一条指令说明。
 *
 * 这里没有直接持有权限、回调等业务对象，只保存最终要画到图片上的文本。
 * 这样渲染器只关心“文字和排版”，不会和命令系统耦合在一起。
 */
data class HelpCommandItem(
    /** 指令文本，例如“签到 / sign”。 */
    val command: String,
    /** 指令右侧的简短说明。 */
    val description: String,
)

/**
 * 帮助图里的一个分组，例如“个人模块”“经济与银行”。
 *
 * renderHelp 会把多个 HelpSection 自动分配到左右两列。
 */
data class HelpSection(
    /** 分组标题。 */
    val title: String,
    /** 当前分组下要展示的指令列表。 */
    val items: List<HelpCommandItem>,
)

/**
 * 个人信息图中“银行存款”面板的一行数据。
 */
data class BankDepositLine(
    /** 银行展示名。 */
    val bankName: String,
    /** 已格式化好的金额文本，例如 1.52w。 */
    val amount: String,
    /**
     * 原始金额数值，用来排序。
     *
     * 图片上画的是 amount，但排序用字符串会出错，例如 "9.8w" 和 "88.6w"。
     */
    val amountValue: Double = 0.0,
)

/**
 * 个人信息图的完整输入模型。
 *
 * Manager / Usecase 负责把业务数据整理成这个结构，渲染器只负责把它画出来。
 * 这种做法可以让图片样式调整时不影响业务逻辑。
 */
data class PersonalInfoCard(
    /** 用户 QQ。 */
    val qq: String,
    /** 用户昵称。 */
    val nickname: String,
    /** 当前称号文本。 */
    val title: String,
    /** 称号起始色，非渐变模式下也会使用这个颜色。 */
    val titleStartColor: Color,
    /** 称号结束色，仅在 titleGradient 为 true 时使用。 */
    val titleEndColor: Color,
    /** 称号是否使用渐变文字。 */
    val titleGradient: Boolean,
    /** 昵称起始色，非渐变模式下暂时不使用，保留给后续样式扩展。 */
    val nicknameStartColor: Color,
    /** 昵称结束色，仅在 nicknameGradient 为 true 时使用。 */
    val nicknameEndColor: Color,
    /** 昵称是否使用渐变文字。 */
    val nicknameGradient: Boolean,
    /** 上次签到时间，作为长文本单独绘制。 */
    val signTime: String,
    /** 连续签到天数。 */
    val signDays: String,
    /** 钱包金币文本。 */
    val wallet: String,
    /** 签到累计收益文本。 */
    val signEarnings: String,
    /** 银行收益文本。 */
    val bankEarnings: String,
    /** 当前所在位置。 */
    val location: String,
    /** 钓鱼冷却状态。 */
    val fishingCooldown: String,
    /** 农场状态概览。 */
    val farmStatus: String,
    /** 各银行存款列表。 */
    val bankDeposits: List<BankDepositLine>,
    /** 右下角信息卡标题，例如“一言”或“签到信息”。 */
    val infoTitle: String,
    /** 右下角信息卡正文，允许包含换行。 */
    val infoText: String,
    /** 信息署名；为空时不绘制署名区域。 */
    val infoSignature: String = "",
)

/**
 * 银行信息图里的资金位置行。
 */
data class BankInfoFundLine(
    val label: String,
    val amount: String,
    val description: String = "",
)

/**
 * 银行信息图里的放贷/额度信息行。
 */
data class BankInfoLoanLine(
    val label: String,
    val value: String,
    val description: String = "",
)

/**
 * 银行信息图输入模型。
 */
data class PrivateBankInfoCard(
    val name: String,
    val code: String,
    val slogan: String,
    val owner: String,
    val star: Int,
    val interest: String,
    val avgReview: String,
    val totalDeposit: String,
    val withdrawSuccessRate: String,
    val defaulterUntil: String,
    val fundLines: List<BankInfoFundLine>,
    val loanLines: List<BankInfoLoanLine>,
)

/**
 * 统一的图片渲染器。
 *
 * 本文件主要使用 Java AWT 绘制图片：先创建 BufferedImage，再通过 Graphics2D
 * 依次绘制背景、面板、文字、装饰元素，最后返回图片给调用方发送。
 */
object EconomyImageRenderer {
    /** 个人信息图宽度，外部背景裁切也会用到。 */
    const val PERSONAL_WIDTH = 1280
    /** 个人信息图高度。 */
    const val PERSONAL_HEIGHT = 720

    // 帮助图是正方形，适合容纳两列指令分组。
    private const val HELP_WIDTH = 1280
    private const val HELP_HEIGHT = 1280
    private const val BANK_INFO_WIDTH = 1280
    private const val BANK_INFO_HEIGHT = 720

    // 个人信息图采用左右两列布局，下面这些常量集中控制主要面板位置。
    private const val PERSONAL_LEFT_X = 56
    private const val PERSONAL_RIGHT_X = 708
    private const val PERSONAL_TOP_Y = 74
    private const val PERSONAL_STATS_Y = 336
    private const val PERSONAL_QUOTE_Y = 416

    // 面板高度抽成常量，后续调版时不用在多个函数里找魔法数字。
    private const val PERSONAL_STATS_HEIGHT = 300
    private const val PERSONAL_BANK_HEIGHT = 314
    private const val PERSONAL_INFO_HEIGHT = 220
    private const val PERSONAL_PANEL_RADIUS = 18

    // 统一色板：背景、正文、辅助文字、线框和常用强调色都在这里维护。
    private val bgTop = Color(247, 250, 252)
    private val bgBottom = Color(232, 238, 240)
    private val ink = Color(34, 40, 49)
    private val muted = Color(101, 112, 123)
    private val line = Color(213, 220, 225)
    private val panel = Color(255, 255, 255, 212)
    private val green = Color(63, 141, 108)
    private val gold = Color(198, 139, 54)
    private val blue = Color(65, 111, 160)
    private val red = Color(190, 83, 83)

    /**
     * 生成主帮助图。
     *
     * @param font 调用方可传入自定义字体，默认使用系统微软雅黑 UI。
     */
    @JvmStatic
    fun renderMainHelp(font: Font = defaultFont()): BufferedImage {
        return renderHelp(
            "HuYanEconomy 帮助",
            "基础经济、个人资产、银行与农场模块",
            mainHelpSections(),
            font
        )
    }

    /**
     * 生成游戏帮助图。
     *
     * 主帮助和游戏帮助共用 renderHelp，只是标题、副标题和分组数据不同。
     */
    @JvmStatic
    fun renderGameHelp(font: Font = defaultFont()): BufferedImage {
        return renderHelp(
            "HuYanEconomy 游戏帮助",
            "钓鱼、抢劫、红包、猜签、抽奖与农场玩法",
            gameHelpSections(),
            font
        )
    }

    /**
     * 生成银行信息图。
     *
     * 银行信息比普通文本更适合分区展示：基础信息、资金位置、额度和放贷状态各自成块。
     */
    @JvmStatic
    fun renderPrivateBankInfo(card: PrivateBankInfoCard, font: Font = defaultFont()): BufferedImage {
        val image = BufferedImage(BANK_INFO_WIDTH, BANK_INFO_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(image)
        drawFlatBackground(g, BANK_INFO_WIDTH, BANK_INFO_HEIGHT)

        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, card.name, 640, 48f))
        g.color = ink
        g.drawString(card.name, 64, 86)
        g.font = font.deriveFont(Font.PLAIN, 22f)
        g.color = muted
        g.drawString("code ${card.code}", 68, 124)

        drawPanel(g, 56, 160, 410, 458)
        drawPrivateBankSummary(g, card, font)

        drawPanel(g, 494, 74, 730, 294)
        drawPrivateBankFunds(g, card, font)

        drawPanel(g, 494, 396, 730, 222)
        drawPrivateBankLoans(g, card, font)

        drawFooter(g, font, BANK_INFO_WIDTH - 32, BANK_INFO_HEIGHT - 24)
        g.dispose()
        return image
    }

    /**
     * 生成个人信息图。
     *
     * @param card 已整理好的用户展示数据。
     * @param avatar 用户头像，可为空；为空时绘制占位块。
     * @param background 用户背景图，可为空；为空时使用默认渐变背景。
     * @param font 绘制文字使用的基础字体。
     */
    @JvmStatic
    fun renderPersonalInfo(card: PersonalInfoCard, avatar: BufferedImage?, background: BufferedImage?, font: Font = defaultFont()): BufferedImage {
        // 个人信息图的主流程只做编排：创建画布、按视觉层级绘制、释放 Graphics2D。
        // 所有具体坐标都收束在各个 drawPersonal... 方法里，避免后续调版时互相牵连。
        val image = BufferedImage(PERSONAL_WIDTH, PERSONAL_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(image)

        // 1. 背景层：先放用户自定义背景或默认柔和底色，再叠加低透明装饰形状。
        drawPersonalBackground(g, background)

        // 2. 内容层：左上身份卡、左下资产卡、右上银行卡、右下信息卡。
        drawPersonalHeader(g, card, avatar, font)
        drawPersonalStats(g, card, font)
        drawPersonalBankPanel(g, card, font)
        drawPersonalQuote(g, card, font)

        // 3. 署名层：固定贴近右下角，保持和其他图片输出一致。
        drawFooter(g, font, PERSONAL_WIDTH - 32, PERSONAL_HEIGHT - 24)
        g.dispose()
        return image
    }

    /**
     * 生成一张本地预览用的个人信息图。
     *
     * 这个方法不访问真实数据库，也不下载真实头像；src/test/java/Test.java 会用它生成预览图片。
     */
    @JvmStatic
    fun previewPersonalInfo(font: Font = defaultFont()): BufferedImage {
        // 先手工画一个头像占位图，避免预览依赖网络。
        val avatar = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(avatar)
        g.paint = GradientPaint(0f, 0f, Color(80, 126, 160), 128f, 128f, Color(226, 175, 95))
        g.fillOval(0, 0, 128, 128)
        g.font = font.deriveFont(Font.BOLD, 52f)
        g.color = Color.WHITE
        g.drawString("壶", 39, 82)
        g.dispose()

        // 使用一组覆盖常见场景的假数据：长银行名、多银行、签到事件、多行文本。
        return renderPersonalInfo(
            PersonalInfoCard(
                qq = "572490972",
                nickname = "放空",
                title = "[是放空!]",
                titleStartColor = Color(98, 39, 116),
                titleEndColor = Color(197, 51, 100),
                titleGradient = true,
                nicknameStartColor = Color(68, 138, 255),
                nicknameEndColor = Color(100, 255, 218),
                nicknameGradient = true,
                signTime = "2026-07-02 20:35:12",
                signDays = "21 天",
                wallet = "512.1",
                signEarnings = "8.6w",
                bankEarnings = "298.9",
                location = "鱼塘",
                fishingCooldown = "还需 4 分钟",
                farmStatus = "有成熟",
                bankDeposits = listOf(
                    BankDepositLine("主银行", "1.52w", 15200.0),
                    BankDepositLine("狐言中央银行", "88.6w", 886000.0),
                    BankDepositLine("南山储蓄社", "12.4w", 124000.0),
                    BankDepositLine("北城联合银行", "9.8w", 98000.0),
                    BankDepositLine("海湾基金库", "7.2w", 72000.0),
                    BankDepositLine("云巷储蓄行", "5.5w", 55000.0)
                ),
                infoTitle = "签到信息",
                infoText = "签到成功!\n金币:960(+960)\n哇偶,你今天运气爆棚,获得160.0金币\n本次签到触发事件:\n装备签到狂人称号，本次签到奖励翻倍!\n已启用签到月卡,本次签到奖励翻5倍!\n使用了一张双倍签到卡，本次签到奖励翻倍!\n使用了一张补签卡，续上断掉的签到天数!",
                infoSignature = ""
            ),
            avatar,
            null,
            font
        )
    }

    /**
     * 帮助图的通用渲染方法。
     *
     * 两张帮助图的视觉结构一致：顶部标题区 + 左右两列分组卡片 + 右下角页脚。
     */
    private fun renderHelp(title: String, subtitle: String, sections: List<HelpSection>, font: Font): BufferedImage {
        val image = BufferedImage(HELP_WIDTH, HELP_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(image)
        drawFlatBackground(g, HELP_WIDTH, HELP_HEIGHT)

        // 顶部标题和副标题先画，给下面的分组卡片留出固定起始位置。
        g.font = font.deriveFont(Font.BOLD, 54f)
        g.color = ink
        g.drawString(title, 72, 96)
        g.font = font.deriveFont(Font.PLAIN, 25f)
        g.color = muted
        g.drawString(subtitle, 76, 138)

        g.font = font.deriveFont(Font.PLAIN, 18f)
        g.color = muted
        g.drawString("版本 ${EconomyBuildConstants.VERSION}", HELP_WIDTH - 200, 96)

        // 两列瀑布流：每次把下一个分组放到当前更短的一列，避免一边太空。
        val columns = listOf(72, 660)
        val yStart = intArrayOf(190, 190)
        val sectionColors = listOf(green, blue, gold, red, Color(91, 108, 143), Color(127, 98, 149))
        sections.forEachIndexed { index, section ->
            val col = if (yStart[0] <= yStart[1]) 0 else 1
            val x = columns[col]
            val y = yStart[col]

            // 卡片高度根据指令数量计算：标题区域 72px，每条指令 42px。
            val height = 72 + section.items.size * 42
            drawPanel(g, x, y, 548, height)

            // 每个分组左侧用不同色条，帮助用户快速区分模块。
            val color = sectionColors[index % sectionColors.size]
            g.color = color
            g.fillRoundRect(x + 24, y + 24, 8, 30, 8, 8)
            g.font = font.deriveFont(Font.BOLD, 28f)
            g.drawString(section.title, x + 46, y + 49)

            var itemY = y + 92
            section.items.forEach { item ->
                // 指令和说明分别限制宽度，文字过长时自动缩小字号。
                g.font = font.deriveFont(Font.BOLD, fitFontSize(g, item.command, 220, 21f))
                g.color = ink
                g.drawString(item.command, x + 28, itemY)
                g.font = font.deriveFont(Font.PLAIN, fitFontSize(g, item.description, 250, 20f))
                g.color = muted
                g.drawString(item.description, x + 270, itemY)
                itemY += 42
            }

            // 记录这一列新的底部位置，下一张卡片会从这里往下排。
            yStart[col] = y + height + 24
        }

        drawFooter(g, font, HELP_WIDTH - 32, HELP_HEIGHT - 28)
        g.dispose()
        return image
    }

    private fun drawPrivateBankSummary(g: Graphics2D, card: PrivateBankInfoCard, font: Font) {
        g.font = font.deriveFont(Font.BOLD, 28f)
        g.color = ink
        g.drawString("银行概况", 86, 210)

        val starText = "★".repeat(card.star.coerceIn(1, 5))
        g.font = font.deriveFont(Font.BOLD, 24f)
        g.color = gold
        g.drawString(starText, 86, 254)
        g.font = font.deriveFont(Font.PLAIN, 18f)
        g.color = muted
        g.drawString("星级 ${card.star.coerceIn(1, 5)} / 平均评分 ${card.avgReview}", 86, 284)

        drawInfoPair(g, font, "行长", card.owner, 86, 330, 320)
        drawInfoPair(g, font, "存款利率", card.interest, 86, 392, 140)
        drawInfoPair(g, font, "存款总额", card.totalDeposit, 246, 392, 170)
        drawInfoPair(g, font, "取款成功率", card.withdrawSuccessRate, 86, 454, 150)
        drawInfoPair(g, font, "失信至", card.defaulterUntil, 246, 454, 170)

        g.font = font.deriveFont(Font.PLAIN, 19f)
        g.color = muted
        g.drawString("银行描述", 86, 516)
        g.font = font.deriveFont(Font.PLAIN, 20f)
        g.color = ink
        drawWrapped(g, card.slogan.ifBlank { "暂无描述" }, 86, 548, 332, 25, 2)
    }

    private fun drawPrivateBankFunds(g: Graphics2D, card: PrivateBankInfoCard, font: Font) {
        g.font = font.deriveFont(Font.BOLD, 28f)
        g.color = ink
        g.drawString("资金位置", 526, 124)

        val colors = listOf(green, blue, gold, red, Color(91, 108, 143), Color(127, 98, 149))
        val lines = card.fundLines.take(6)
        lines.forEachIndexed { index, line ->
            val col = index % 2
            val row = index / 2
            val x = 526 + col * 350
            val y = 174 + row * 68
            val color = colors[index % colors.size]
            g.color = Color(color.red, color.green, color.blue, 34)
            g.fillRoundRect(x, y - 30, 306, 54, 14, 14)
            g.color = color
            g.fillRoundRect(x + 14, y - 18, 8, 32, 8, 8)
            g.font = font.deriveFont(Font.PLAIN, 17f)
            g.color = muted
            g.drawString(line.label, x + 34, y - 8)
            g.font = font.deriveFont(Font.BOLD, fitFontSize(g, line.amount, 180, 24f))
            g.color = ink
            g.drawString(line.amount, x + 34, y + 20)
            if (line.description.isNotBlank()) {
                g.font = font.deriveFont(Font.PLAIN, 15f)
                g.color = muted
                g.drawString(line.description.take(18), x + 176, y + 20)
            }
        }
    }

    private fun drawPrivateBankLoans(g: Graphics2D, card: PrivateBankInfoCard, font: Font) {
        g.font = font.deriveFont(Font.BOLD, 28f)
        g.color = ink
        g.drawString("额度与放贷", 526, 446)

        val lines = if (card.loanLines.isEmpty()) {
            listOf(BankInfoLoanLine("暂无放贷", "0", "未发布贷款额度"))
        } else {
            card.loanLines
        }
        var y = 494
        lines.take(4).forEach { line ->
            g.font = font.deriveFont(Font.PLAIN, 18f)
            g.color = muted
            g.drawString(line.label, 526, y)
            g.font = font.deriveFont(Font.BOLD, fitFontSize(g, line.value, 190, 24f))
            g.color = ink
            g.drawString(line.value, 678, y)
            if (line.description.isNotBlank()) {
                g.font = font.deriveFont(Font.PLAIN, 17f)
                g.color = muted
                drawWrapped(g, line.description, 888, y, 284, 21, 1)
            }
            y += 42
        }
    }

    private fun drawInfoPair(g: Graphics2D, font: Font, label: String, value: String, x: Int, y: Int, maxWidth: Int) {
        g.font = font.deriveFont(Font.PLAIN, 17f)
        g.color = muted
        g.drawString(label, x, y)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, value, maxWidth, 22f))
        g.color = ink
        g.drawString(value, x, y + 30)
    }

    /**
     * 绘制个人信息图背景。
     *
     * 自定义背景需要先按 cover 方式铺满画布，再叠一层半透明浅色遮罩，
     * 这样背景不会抢走文字层级，同时也不会变成一整张灰白雾面。
     */
    private fun drawPersonalBackground(g: Graphics2D, background: BufferedImage?) {
        // 自定义底图采用 cover 裁切策略：保证填满 1280x720，并从中心裁切多余部分。
        if (background != null) {
            val scale = maxOf(PERSONAL_WIDTH.toDouble() / background.width, PERSONAL_HEIGHT.toDouble() / background.height)
            val width = (background.width * scale).toInt()
            val height = (background.height * scale).toInt()
            val x = (PERSONAL_WIDTH - width) / 2
            val y = (PERSONAL_HEIGHT - height) / 2
            g.drawImage(background, x, y, width, height, null)

            // 轻压背景亮度即可，避免整张图被白雾盖住。
            g.composite = AlphaComposite.SrcOver.derive(0.34f)
            g.paint = GradientPaint(
                0f,
                0f,
                Color(239, 247, 248),
                PERSONAL_WIDTH.toFloat(),
                PERSONAL_HEIGHT.toFloat(),
                Color(210, 226, 231)
            )
            g.fillRect(0, 0, PERSONAL_WIDTH, PERSONAL_HEIGHT)
            g.composite = AlphaComposite.SrcOver
        } else {
            drawFlatBackground(g, PERSONAL_WIDTH, PERSONAL_HEIGHT)
        }

        // 两个低透明圆形用于压住空白区的视觉重心，不承载信息。
        g.color = Color(255, 255, 255, 46)
        g.fillOval(910, -150, 420, 420)
        g.color = Color(202, 217, 212, 40)
        g.fillOval(-140, 430, 350, 350)
    }

    /**
     * 绘制左上角身份卡。
     *
     * 这个面板承担“用户是谁”的信息：头像、昵称、称号、QQ、位置/钓鱼/农场状态。
     */
    private fun drawPersonalHeader(g: Graphics2D, card: PersonalInfoCard, avatar: BufferedImage?, font: Font) {
        // 身份卡是第一视觉入口：头像、昵称、称号、QQ 和三个状态标签都放在这里。
        drawPanel(g, PERSONAL_LEFT_X, PERSONAL_TOP_Y, 620, 234)

        // 头像统一缩放成 128 方图并裁圆角；没有头像时使用浅灰占位块。
        val avatarImage = avatar?.let { ImageUtil.makeRoundedCorner(resize(it, 128, 128), 42) }
        if (avatarImage != null) {
            g.drawImage(avatarImage, 92, PERSONAL_TOP_Y + 36, null)
        } else {
            g.color = Color(220, 226, 230)
            g.fillRoundRect(92, PERSONAL_TOP_Y + 36, 128, 128, 42, 42)
        }

        // 昵称按可用宽度动态压缩字号，长昵称不挤出面板。
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, card.nickname, 350, 52f))
        if (card.nicknameGradient) {
            ImageUtil.drawStringGradient(card.nickname, 250, PERSONAL_TOP_Y + 80, card.nicknameStartColor, card.nicknameEndColor, g)
        } else {
            g.color = ink
            g.drawString(card.nickname, 250, PERSONAL_TOP_Y + 80)
        }

        // 称号沿用用户称号配置里的颜色和渐变设置。
        g.font = font.deriveFont(Font.PLAIN, 24f)
        if (card.titleGradient) {
            ImageUtil.drawStringGradient(card.title, 252, PERSONAL_TOP_Y + 118, card.titleStartColor, card.titleEndColor, g)
        } else {
            g.color = card.titleStartColor
            g.drawString(card.title, 252, PERSONAL_TOP_Y + 118)
        }

        g.font = font.deriveFont(Font.PLAIN, 22f)
        g.color = muted
        g.drawString("QQ ${card.qq}", 252, PERSONAL_TOP_Y + 155)

        // 三个 chip 固定展示新增状态：当前位置、钓鱼冷却、农场概况。
        val chipY = PERSONAL_TOP_Y + 186
        drawChip(g, 92, chipY, "位置", card.location, green, font)
        drawChip(g, 257, chipY, "钓鱼", card.fishingCooldown, blue, font, width = 178)
        drawChip(g, 462, chipY, "农场", card.farmStatus, gold, font)
    }

    /**
     * 绘制左下角资产概览卡。
     *
     * 四个核心数值使用两列网格，上次签到时间因为通常较长，单独放在底部。
     */
    private fun drawPersonalStats(g: Graphics2D, card: PersonalInfoCard, font: Font) {
        // 资产卡使用两列指标布局，底部独立放“上次签到”长文本。
        drawPanel(g, PERSONAL_LEFT_X, PERSONAL_STATS_Y, 620, PERSONAL_STATS_HEIGHT)
        g.font = font.deriveFont(Font.BOLD, 28f)
        g.color = ink
        g.drawString("资产概览", 86, PERSONAL_STATS_Y + 46)
        drawStat(g, font, "钱包金币", card.wallet, 86, PERSONAL_STATS_Y + 92, 210)
        drawStat(g, font, "签到收益", card.signEarnings, 366, PERSONAL_STATS_Y + 92, 210)
        drawStat(g, font, "银行收益", card.bankEarnings, 86, PERSONAL_STATS_Y + 160, 210)
        drawStat(g, font, "连续签到", card.signDays, 366, PERSONAL_STATS_Y + 160, 210)

        // 长日期比数值更容易挤压，所以单独占一行，并比指标组略微下沉。
        val lastSignY = PERSONAL_STATS_Y + 272
        g.font = font.deriveFont(Font.PLAIN, 19f)
        g.color = muted
        g.drawString("上次签到", 86, lastSignY)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, card.signTime, 380, 24f))
        g.color = ink
        g.drawString(card.signTime, 190, lastSignY)
    }

    /**
     * 绘制一个“标签 + 大号数值”的资产指标。
     *
     * @param maxWidth 数值允许占用的最大宽度，超过时通过 fitFontSize 缩小字号。
     */
    private fun drawStat(g: Graphics2D, font: Font, label: String, value: String, x: Int, y: Int, maxWidth: Int) {
        // 先画浅色标签，再画较粗的数值，形成清晰的信息层级。
        g.font = font.deriveFont(Font.PLAIN, 21f)
        g.color = muted
        g.drawString(label, x, y)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, value, maxWidth, 31f))
        g.color = ink
        g.drawString(value, x, y + 34)
    }

    /**
     * 绘制右上角银行存款卡。
     *
     * 银行列表可能很多，所以这里只展示金额最高的前几项，剩余项用一句话折叠。
     */
    private fun drawPersonalBankPanel(g: Graphics2D, card: PersonalInfoCard, font: Font) {
        // 银行卡强调“银行名称 -> 金额”的对应关系，右侧金额右对齐方便扫读。
        drawPanel(g, PERSONAL_RIGHT_X, PERSONAL_TOP_Y, 516, PERSONAL_BANK_HEIGHT)
        g.font = font.deriveFont(Font.BOLD, 28f)
        g.color = ink
        g.drawString("银行存款", 740, PERSONAL_TOP_Y + 46)

        // 银行数量过多时只展示存款最高的前 4 个，剩余数量折叠在卡片底部。
        val deposits = if (card.bankDeposits.isEmpty()) {
            listOf(BankDepositLine("暂无存款", "0"))
        } else {
            card.bankDeposits.sortedByDescending { it.amountValue }
        }
        var y = PERSONAL_TOP_Y + 96
        deposits.take(4).forEachIndexed { index, line ->
            val color = listOf(green, blue, gold, red, Color(91, 108, 143))[index % 5]

            // 左侧竖色条用来分隔每一行，也让纯文字列表不那么单调。
            g.color = color
            g.fillRoundRect(742, y - 20, 10, 34, 10, 10)

            // 银行名靠左，金额靠右：这是列表型金额信息最容易扫读的布局。
            g.font = font.deriveFont(Font.BOLD, fitFontSize(g, line.bankName, 255, 24f))
            g.color = ink
            g.drawString(line.bankName, 766, y)
            g.font = font.deriveFont(Font.BOLD, fitFontSize(g, line.amount, 145, 26f))
            g.color = color

            // Graphics2D 只有 drawString(x, y)，没有“右对齐”参数，所以需要手动减去文字宽度。
            val amountWidth = g.fontMetrics.stringWidth(line.amount)
            g.drawString(line.amount, 1182 - amountWidth, y)
            y += 45
        }
        if (card.bankDeposits.size > 4) {
            g.font = font.deriveFont(Font.PLAIN, 19f)
            g.color = muted
            g.drawString("另有 ${card.bankDeposits.size - 4} 个银行未展示", 742, PERSONAL_TOP_Y + 290)
        }
    }

    /**
     * 绘制右下角信息卡。
     *
     * 这里有两个用途：
     * 1. 普通个人信息图展示“一言”或其他补充信息。
     * 2. 签到成功后展示签到摘要和触发事件。
     */
    private fun drawPersonalQuote(g: Graphics2D, card: PersonalInfoCard, font: Font) {
        // 右下信息卡是可复用的信息展示区：
        // 普通个人信息展示“一言”，签到时展示签到结果和触发事件。
        drawPanel(g, PERSONAL_RIGHT_X, PERSONAL_QUOTE_Y, 516, PERSONAL_INFO_HEIGHT)

        // 签到信息结构更复杂，交给专门函数做小报表式排版。
        if (card.infoTitle == "签到信息") {
            drawSignInfo(g, card.infoText, font)
            return
        }

        g.font = font.deriveFont(Font.BOLD, 26f)
        g.color = ink
        g.drawString(card.infoTitle, 740, PERSONAL_QUOTE_Y + 46)

        // 有署名时通常是“一言”场景，正文少一些，给署名留位置；
        // 签到场景不传署名，正文区可展示 6 行左右的签到结果和事件列表。
        val hasSignature = card.infoSignature.isNotBlank()
        val bodyFontSize = if (hasSignature) 22f else 20f
        val bodyLineHeight = if (hasSignature) 25 else 22
        val bodyMaxLines = if (hasSignature) 4 else 6

        g.font = font.deriveFont(Font.PLAIN, bodyFontSize)
        g.color = muted
        drawWrapped(g, card.infoText, 740, PERSONAL_QUOTE_Y + 82, 452, bodyLineHeight, bodyMaxLines)
        if (hasSignature) {
            g.font = font.deriveFont(Font.PLAIN, 18f)
            g.color = Color(120, 130, 140)
            g.drawString(card.infoSignature.take(34), 740, PERSONAL_QUOTE_Y + 178)
        }
    }

    /**
     * 绘制签到信息卡内容。
     *
     * text 来自原本的文本消息，格式大致是：
     * 签到成功 / 金币变化 / 运气提示 / 本次签到触发事件: / 多条事件。
     * 这里会把它拆成“摘要区”和“事件区”，让图片比纯文本更紧凑。
     */
    private fun drawSignInfo(g: Graphics2D, text: String, font: Font) {
        // 签到信息比“一言”更像一份小报表：先展示结果摘要，再展示触发事件。
        // “本次签到触发事件:”保留给文本回退消息，图片里改用更短的标签减少拥挤。
        val lines = text
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val eventTitleIndex = lines.indexOf("本次签到触发事件:")
        val summaryLines = if (eventTitleIndex >= 0) lines.take(eventTitleIndex) else lines
        val eventLines = if (eventTitleIndex >= 0) lines.drop(eventTitleIndex + 1) else emptyList()

        val resultLine = summaryLines.getOrNull(0) ?: "签到成功!"
        val moneyLine = summaryLines.getOrNull(1).orEmpty()
        val bonusLines = summaryLines.drop(2)
        val visibleEvents = eventLines.take(if (eventLines.size > 3) 2 else 3).toMutableList()
        if (eventLines.size > 3) {
            // 事件太多时不要硬塞进卡片，否则会和页脚或卡片边缘重叠。
            visibleEvents += "另有 ${eventLines.size - 2} 条触发事件"
        }

        g.font = font.deriveFont(Font.BOLD, 26f)
        g.color = green
        g.drawString(resultLine.removeSuffix("!"), 740, PERSONAL_QUOTE_Y + 46)

        if (moneyLine.isNotBlank()) {
            g.font = font.deriveFont(Font.BOLD, 19f)
            g.color = ink
            // 金币行包含“余额”和“本次新增”，单独染色能突出收益。
            drawMoneyLine(g, moneyLine, 740, PERSONAL_QUOTE_Y + 76, font)
        }

        if (bonusLines.isNotEmpty()) {
            g.font = font.deriveFont(Font.PLAIN, 18f)
            g.color = muted
            drawWrapped(g, bonusLines.joinToString("，"), 740, PERSONAL_QUOTE_Y + 101, 452, 20, 1)
        }

        drawSectionTitle(g, "触发事件", 740, PERSONAL_QUOTE_Y + 130, gold, font)
        g.font = font.deriveFont(Font.PLAIN, 16f)
        g.color = muted
        val eventText = visibleEvents.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "无额外事件"
        drawWrapped(g, eventText, 740, PERSONAL_QUOTE_Y + 157, 452, 18, 3)
    }

    /**
     * 绘制金币变化行。
     *
     * 输入常见格式为“金币:960(+960)”。如果没有找到“(+”，就按普通文本绘制。
     */
    private fun drawMoneyLine(g: Graphics2D, moneyLine: String, x: Int, y: Int, font: Font) {
        // 金币行通常形如“金币:960(+960)”，把本次增量染成绿色，方便一眼看到收益。
        val rewardStart = moneyLine.indexOf("(+")
        if (rewardStart < 0) {
            g.drawString(moneyLine, x, y)
            return
        }

        val balance = moneyLine.substring(0, rewardStart)
        val reward = moneyLine.substring(rewardStart).removePrefix("(").removeSuffix(")")
        g.font = font.deriveFont(Font.PLAIN, 18f)
        g.color = muted
        val balanceWidth = g.fontMetrics.stringWidth(balance)
        g.drawString(balance, x, y)
        g.font = font.deriveFont(Font.BOLD, 20f)
        g.color = green
        g.drawString(reward, x + balanceWidth + 12, y)
    }

    /**
     * 绘制一个短小的章节标题，例如“触发事件”。
     */
    private fun drawSectionTitle(g: Graphics2D, text: String, x: Int, y: Int, color: Color, font: Font) {
        g.color = color
        g.font = font.deriveFont(Font.BOLD, 17f)
        g.drawString(text, x, y)
    }

    /**
     * 绘制默认背景。
     *
     * 没有用户自定义背景时使用浅色渐变，并在顶部加一条微弱高光。
     */
    private fun drawFlatBackground(g: Graphics2D, width: Int, height: Int) {
        // GradientPaint 会在两个点之间插值颜色，这里从左上过渡到右下。
        g.paint = GradientPaint(0f, 0f, bgTop, width.toFloat(), height.toFloat(), bgBottom)
        g.fillRect(0, 0, width, height)
        g.color = Color(255, 255, 255, 90)
        g.fillRect(0, 0, width, 8)
    }

    /**
     * 绘制通用圆角面板。
     *
     * 顺序很重要：先画阴影，再画一点内高光，再画主体填充，最后画边框。
     */
    private fun drawPanel(g: Graphics2D, x: Int, y: Int, w: Int, h: Int) {
        // 低透明阴影稍微向右下偏移，让卡片从背景里浮出来。
        g.color = Color(36, 52, 66, 22)
        g.fillRoundRect(x + 3, y + 5, w, h, PERSONAL_PANEL_RADIUS, PERSONAL_PANEL_RADIUS)

        // 主体使用接近不透明的白色，保证复杂背景上也能读清文字。
        g.color = panel
        g.fillRoundRect(x, y, w, h, PERSONAL_PANEL_RADIUS, PERSONAL_PANEL_RADIUS)

        // 叠一层非常淡的高光，模拟玻璃面板的上层反光。
        g.color = Color(255, 255, 255, 18)
        g.fillRoundRect(x + 1, y + 1, w - 2, h - 2, PERSONAL_PANEL_RADIUS, PERSONAL_PANEL_RADIUS)

        // RoundRectangle2D + stroke 用于画更稳定的圆角边框。
        g.color = Color(174, 193, 204, 176)
        g.stroke = BasicStroke(1.2f)
        g.draw(RoundRectangle2D.Double(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble(), PERSONAL_PANEL_RADIUS.toDouble(), PERSONAL_PANEL_RADIUS.toDouble()))
    }

    /**
     * 绘制身份卡底部的小状态标签。
     *
     * 结构是：浅色背景块 + 彩色圆点 + 标签名 + 状态值。
     */
    private fun drawChip(g: Graphics2D, x: Int, y: Int, label: String, value: String, color: Color, font: Font, width: Int = 138) {
        // 背景使用强调色的低 alpha 版本，既能归类又不会太抢眼。
        g.color = Color(color.red, color.green, color.blue, 30)
        g.fillRoundRect(x, y, width, 36, 16, 16)
        g.color = color
        g.fillRoundRect(x + 10, y + 11, 14, 14, 14, 14)
        g.font = font.deriveFont(Font.PLAIN, 15f)
        g.drawString(label, x + 30, y + 23)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, value, width - 64, 17f))
        g.color = ink
        g.drawString(value, x + 68, y + 24)
    }

    /**
     * 绘制右下角页脚。
     *
     * @param rightX 页脚右边界，而不是左上角 x。这样版本号变长时仍能右对齐。
     */
    private fun drawFooter(g: Graphics2D, font: Font, rightX: Int, y: Int) {
        val text = "by Mirai + Overflow & HuYanEconomy(壶言经济) v${EconomyBuildConstants.VERSION}"
        g.font = font.deriveFont(Font.PLAIN, 17f)
        g.color = Color(120, 130, 140)
        g.drawString(text, rightX - g.fontMetrics.stringWidth(text), y)
    }

    /**
     * 按像素宽度手动换行并绘制文本。
     *
     * AWT 的 drawString 不会自动处理中文换行，所以这里逐字符累积，
     * 每次超过 maxWidth 就把当前行画出去。
     */
    private fun drawWrapped(g: Graphics2D, text: String, x: Int, y: Int, maxWidth: Int, lineHeight: Int, maxLines: Int) {
        // AWT 没有内置的中文自动换行，这里按字符测量宽度。
        // 先尊重调用方传入的显式换行，再在单行超宽时拆成多行，最多绘制 maxLines 行。
        var currentY = y
        var lines = 0
        for (paragraph in text.split('\n')) {
            if (paragraph.isBlank()) {
                currentY += lineHeight
                lines += 1
                if (lines >= maxLines) return
                continue
            }

            var line = ""
            for (char in paragraph) {
                val next = line + char
                if (g.fontMetrics.stringWidth(next) > maxWidth && line.isNotBlank()) {
                    g.drawString(line, x, currentY)
                    currentY += lineHeight
                    lines += 1
                    if (lines >= maxLines) return
                    line = char.toString()
                } else {
                    line = next
                }
            }
            if (line.isNotBlank()) {
                g.drawString(line, x, currentY)
                currentY += lineHeight
                lines += 1
                if (lines >= maxLines) return
            }
        }
    }

    /**
     * 根据可用宽度动态计算字号。
     *
     * 从 start 开始逐步减小字号，直到文字宽度能放进 maxWidth，最低不小于 12。
     */
    private fun fitFontSize(g: Graphics2D, text: String, maxWidth: Int, start: Float): Float {
        var size = start
        val base = g.font
        while (size > 12f) {
            // getFontMetrics 必须使用当前尝试的字号，否则测出来的宽度不准确。
            val metrics = g.getFontMetrics(base.deriveFont(size))
            if (metrics.stringWidth(text) <= maxWidth) return size
            size -= 1f
        }
        return 12f
    }

    /**
     * 把图片缩放到固定尺寸。
     *
     * 当前主要用于头像：先缩放成正方形，再交给 ImageUtil.makeRoundedCorner 做圆角。
     */
    private fun resize(image: BufferedImage, width: Int, height: Int): BufferedImage {
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(output)
        g.drawImage(image, 0, 0, width, height, null)
        g.dispose()
        return output
    }

    /**
     * 默认字体。
     *
     * 生产环境如果传入了自定义字体，会覆盖这里；这里主要给预览和兜底使用。
     */
    private fun defaultFont(): Font = Font("Microsoft YaHei UI", Font.PLAIN, 24)

    /**
     * 主帮助图的数据源。
     *
     * 这里故意只维护“适合放在图片里的短句”，不要把完整业务说明塞进来。
     * 图片帮助适合快速扫读，详细规则仍应放在具体指令反馈里。
     */
    private fun mainHelpSections(): List<HelpSection> = listOf(
        // 个人模块：围绕用户自己的资产、状态、签到入口。
        HelpSection(
            "个人模块",
            listOf(
                HelpCommandItem("签到 / sign", "每日签到获取金币"),
                HelpCommandItem("个人信息 / info", "查看个人资产卡片"),
                HelpCommandItem("我的资金", "查看钱包与银行余额"),
                HelpCommandItem("我的位置", "查看当前位置状态"),
                HelpCommandItem("回家 / 出院", "位置恢复指令")
            )
        ),
        // 经济与银行：基础金币流转和主银行操作。
        HelpSection(
            "经济与银行",
            listOf(
                HelpCommandItem("转账 @用户 金额", "向其他用户转账"),
                HelpCommandItem("存款!", "钱包余额全部存默认银行"),
                HelpCommandItem("存款!!", "钱包余额全部存主银行"),
                HelpCommandItem("存款 金额 [!/银行]", "存入主银行或指定银行"),
                HelpCommandItem("取款!", "默认银行余额全部取出"),
                HelpCommandItem("取款!!", "主银行余额全部取出"),
                HelpCommandItem("取款 金额 [!/银行]", "从主银行或指定银行取款"),
                HelpCommandItem("银行利率 / 富豪榜", "查询利率与排行")
            )
        ),
        // 私人银行：用户创建的银行、贷款和评分相关指令。
        HelpSection(
            "私人银行",
            listOf(
                HelpCommandItem("银行列表", "查看可用银行"),
                HelpCommandItem("银行创建 code 名称", "创建自己的银行"),
                HelpCommandItem("银行信息 [code]", "查看银行详情"),
                HelpCommandItem("银行评分 1-5 [内容]", "给银行评分"),
                HelpCommandItem("贷款 金额 [银行]", "申请银行贷款"),
                HelpCommandItem("还款 金额 [银行]", "偿还贷款"),
                HelpCommandItem("狐卷 / 狐卷竞标", "查看和竞标狐卷")
            )
        ),
        // 道具与称号：背包消耗品和称号系统。
        HelpSection(
            "道具与称号",
            listOf(
                HelpCommandItem("我的背包", "查看背包道具"),
                HelpCommandItem("道具商店 [页码]", "购买道具"),
                HelpCommandItem("使用 道具 数量", "使用背包道具"),
                HelpCommandItem("我的称号", "查看称号列表"),
                HelpCommandItem("称号商店", "购买称号"),
                HelpCommandItem("切换称号 编号", "切换当前称号")
            )
        ),
        // 其他：只放跨模块入口和私聊绑定类指令。
        HelpSection(
            "其他",
            listOf(
                HelpCommandItem("#fund bind QQ", "私聊绑定资助"),
                HelpCommandItem("#fund get code 金额", "领取资助金币"),
                HelpCommandItem("游戏帮助", "查看游戏指令图")
            )
        )
    )

    /**
     * 游戏帮助图的数据源。
     *
     * 游戏类玩法的指令数量较多，单独成图可以避免主帮助图过长。
     */
    private fun gameHelpSections(): List<HelpSection> = listOf(
        // 钓鱼游戏：开始、收竿、鱼塘和管理员开关。
        HelpSection(
            "钓鱼游戏",
            listOf(
                HelpCommandItem("钓鱼 / 抛竿", "开始钓鱼"),
                HelpCommandItem("拉起!", "浮漂提示后收竿"),
                HelpCommandItem("鱼塘等级", "查看本群鱼塘"),
                HelpCommandItem("刷新钓鱼", "管理员重置状态"),
                HelpCommandItem("开启 / 关闭 钓鱼", "管理员开关")
            )
        ),
        // 抢劫游戏：按武力值和状态判定的对抗玩法。
        HelpSection(
            "抢劫游戏",
            listOf(
                HelpCommandItem("抢劫 @用户", "按武力值判定成功率"),
                HelpCommandItem("抢劫榜", "查看抢劫排行"),
                HelpCommandItem("出院", "从医院恢复"),
                HelpCommandItem("开启 / 关闭 抢劫", "管理员开关")
            )
        ),
        // 红包游戏：群内红包的创建、领取和列表查询。
        HelpSection(
            "红包游戏",
            listOf(
                HelpCommandItem("发红包 金额 数量", "默认均分红包"),
                HelpCommandItem("发红包 金额 数量 sj", "随机红包"),
                HelpCommandItem("抢红包", "领取本群红包"),
                HelpCommandItem("领红包 id", "领取指定红包"),
                HelpCommandItem("红包列表", "查看可领红包")
            )
        ),
        // 猜签与抽奖：概率类金币玩法。
        HelpSection(
            "猜签与抽奖",
            listOf(
                HelpCommandItem("猜签 数字 金额", "模拟彩票玩法"),
                HelpCommandItem("抽奖", "默认奖池抽奖"),
                HelpCommandItem("抽奖 奖池编号", "指定奖池抽奖"),
                HelpCommandItem("切换奖池 编号", "切换默认奖池"),
                HelpCommandItem("奖池列表", "查看奖池"),
                HelpCommandItem("抽奖信息 / 好运榜", "查看统计和排行")
            )
        ),
        // 农场玩法也会出现在游戏帮助里，方便纯娱乐玩家从这里进入。
        HelpSection(
            "农场玩法",
            listOf(
                HelpCommandItem("我的农场", "查看农场状态"),
                HelpCommandItem("农场商店 / 仓库", "种子与库存"),
                HelpCommandItem("购买种子 名称 数量", "购买种子"),
                HelpCommandItem("播种 1,2,3 名称", "指定地块播种"),
                HelpCommandItem("收获 1,2,3", "指定地块收获"),
                HelpCommandItem("一键播种 / 收获", "批量农场操作"),
                HelpCommandItem("一键收获 / 卖出", "批量处理"),
                HelpCommandItem("帮浇水 @用户", "加速好友作物"),
                HelpCommandItem("黑市 / 激活守护", "特殊农场功能")
            )
        )
    )
}
