package cn.chahuyun.economy.utils

import cn.chahuyun.economy.EconomyBuildConstants
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

data class HelpCommandItem(
    val command: String,
    val description: String,
)

data class HelpSection(
    val title: String,
    val items: List<HelpCommandItem>,
)

data class BankDepositLine(
    val bankName: String,
    val amount: String,
)

data class PersonalInfoCard(
    val qq: String,
    val nickname: String,
    val title: String,
    val titleStartColor: Color,
    val titleEndColor: Color,
    val titleGradient: Boolean,
    val nicknameStartColor: Color,
    val nicknameEndColor: Color,
    val nicknameGradient: Boolean,
    val signTime: String,
    val signDays: String,
    val wallet: String,
    val signEarnings: String,
    val bankEarnings: String,
    val location: String,
    val fishingCooldown: String,
    val farmStatus: String,
    val bankDeposits: List<BankDepositLine>,
    val hitokoto: String,
    val signature: String,
)

object EconomyImageRenderer {
    const val PERSONAL_WIDTH = 1280
    const val PERSONAL_HEIGHT = 720

    private const val HELP_WIDTH = 1280
    private const val HELP_HEIGHT = 1280
    private const val PERSONAL_LEFT_X = 56
    private const val PERSONAL_RIGHT_X = 708
    private const val PERSONAL_TOP_Y = 74
    private const val PERSONAL_STATS_Y = 336
    private const val PERSONAL_QUOTE_Y = 436
    private const val PERSONAL_PANEL_RADIUS = 18
    private val bgTop = Color(247, 250, 252)
    private val bgBottom = Color(232, 238, 240)
    private val ink = Color(34, 40, 49)
    private val muted = Color(101, 112, 123)
    private val line = Color(213, 220, 225)
    private val panel = Color(255, 255, 255, 232)
    private val green = Color(63, 141, 108)
    private val gold = Color(198, 139, 54)
    private val blue = Color(65, 111, 160)
    private val red = Color(190, 83, 83)

    @JvmStatic
    fun renderMainHelp(font: Font = defaultFont()): BufferedImage {
        return renderHelp(
            "HuYanEconomy 帮助",
            "基础经济、个人资产、银行与农场模块",
            mainHelpSections(),
            font
        )
    }

    @JvmStatic
    fun renderGameHelp(font: Font = defaultFont()): BufferedImage {
        return renderHelp(
            "HuYanEconomy 游戏帮助",
            "钓鱼、抢劫、红包、猜签、抽奖与农场玩法",
            gameHelpSections(),
            font
        )
    }

    @JvmStatic
    fun renderPersonalInfo(card: PersonalInfoCard, avatar: BufferedImage?, background: BufferedImage?, font: Font = defaultFont()): BufferedImage {
        // 个人信息图的主流程只做编排：创建画布、按视觉层级绘制、释放 Graphics2D。
        // 所有具体坐标都收束在各个 drawPersonal... 方法里，避免后续调版时互相牵连。
        val image = BufferedImage(PERSONAL_WIDTH, PERSONAL_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(image)

        // 1. 背景层：先放用户自定义背景或默认柔和底色，再叠加低透明装饰形状。
        drawPersonalBackground(g, background)

        // 2. 内容层：左上身份卡、左下资产卡、右上银行卡、右下一言卡。
        drawPersonalHeader(g, card, avatar, font)
        drawPersonalStats(g, card, font)
        drawPersonalBankPanel(g, card, font)
        drawPersonalQuote(g, card, font)

        // 3. 署名层：固定贴近右下角，保持和其他图片输出一致。
        drawFooter(g, font, PERSONAL_WIDTH - 395, PERSONAL_HEIGHT - 24)
        g.dispose()
        return image
    }

    @JvmStatic
    fun previewPersonalInfo(font: Font = defaultFont()): BufferedImage {
        val avatar = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(avatar)
        g.paint = GradientPaint(0f, 0f, Color(80, 126, 160), 128f, 128f, Color(226, 175, 95))
        g.fillOval(0, 0, 128, 128)
        g.font = font.deriveFont(Font.BOLD, 52f)
        g.color = Color.WHITE
        g.drawString("壶", 39, 82)
        g.dispose()

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
                    BankDepositLine("主银行", "1.52w"),
                    BankDepositLine("狐言中央银行", "88.6w"),
                    BankDepositLine("南山储蓄社", "12.4w")
                ),
                hitokoto = "愿你今日的金币和好运都在账上稳稳入库。",
                signature = "-- HuYanEconomy"
            ),
            avatar,
            null,
            font
        )
    }

    private fun renderHelp(title: String, subtitle: String, sections: List<HelpSection>, font: Font): BufferedImage {
        val image = BufferedImage(HELP_WIDTH, HELP_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(image)
        drawFlatBackground(g, HELP_WIDTH, HELP_HEIGHT)

        g.font = font.deriveFont(Font.BOLD, 54f)
        g.color = ink
        g.drawString(title, 72, 96)
        g.font = font.deriveFont(Font.PLAIN, 25f)
        g.color = muted
        g.drawString(subtitle, 76, 138)

        g.font = font.deriveFont(Font.PLAIN, 18f)
        g.color = muted
        g.drawString("版本 ${EconomyBuildConstants.VERSION}", HELP_WIDTH - 200, 96)

        val columns = listOf(72, 660)
        val yStart = intArrayOf(190, 190)
        val sectionColors = listOf(green, blue, gold, red, Color(91, 108, 143), Color(127, 98, 149))
        sections.forEachIndexed { index, section ->
            val col = if (yStart[0] <= yStart[1]) 0 else 1
            val x = columns[col]
            val y = yStart[col]
            val height = 72 + section.items.size * 42
            drawPanel(g, x, y, 548, height)
            val color = sectionColors[index % sectionColors.size]
            g.color = color
            g.fillRoundRect(x + 24, y + 24, 8, 30, 8, 8)
            g.font = font.deriveFont(Font.BOLD, 28f)
            g.drawString(section.title, x + 46, y + 49)

            var itemY = y + 92
            section.items.forEach { item ->
                g.font = font.deriveFont(Font.BOLD, fitFontSize(g, item.command, 220, 21f))
                g.color = ink
                g.drawString(item.command, x + 28, itemY)
                g.font = font.deriveFont(Font.PLAIN, fitFontSize(g, item.description, 250, 20f))
                g.color = muted
                g.drawString(item.description, x + 270, itemY)
                itemY += 42
            }
            yStart[col] = y + height + 24
        }

        drawFooter(g, font, HELP_WIDTH - 395, HELP_HEIGHT - 28)
        g.dispose()
        return image
    }

    private fun drawPersonalBackground(g: Graphics2D, background: BufferedImage?) {
        // 自定义底图采用 cover 裁切策略：保证填满 1280x720，并从中心裁切多余部分。
        if (background != null) {
            val scale = maxOf(PERSONAL_WIDTH.toDouble() / background.width, PERSONAL_HEIGHT.toDouble() / background.height)
            val width = (background.width * scale).toInt()
            val height = (background.height * scale).toInt()
            val x = (PERSONAL_WIDTH - width) / 2
            val y = (PERSONAL_HEIGHT - height) / 2
            g.drawImage(background, x, y, width, height, null)

            // 叠一层浅色蒙版，确保任意背景图上文字和面板边界都有稳定可读性。
            g.composite = AlphaComposite.SrcOver.derive(0.78f)
            g.paint = GradientPaint(0f, 0f, Color(250, 252, 252), PERSONAL_WIDTH.toFloat(), PERSONAL_HEIGHT.toFloat(), Color(236, 241, 242))
            g.fillRect(0, 0, PERSONAL_WIDTH, PERSONAL_HEIGHT)
            g.composite = AlphaComposite.SrcOver
        } else {
            drawFlatBackground(g, PERSONAL_WIDTH, PERSONAL_HEIGHT)
        }

        // 两个低透明圆形用于压住空白区的视觉重心，不承载信息。
        g.color = Color(255, 255, 255, 130)
        g.fillOval(910, -150, 420, 420)
        g.color = Color(202, 217, 212, 120)
        g.fillOval(-140, 430, 350, 350)
    }

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
        drawChip(g, 252, chipY, "钓鱼", card.fishingCooldown, blue, font)
        drawChip(g, 445, chipY, "农场", card.farmStatus, gold, font)
    }

    private fun drawPersonalStats(g: Graphics2D, card: PersonalInfoCard, font: Font) {
        // 资产卡使用两列指标布局，底部独立放“上次签到”长文本。
        drawPanel(g, PERSONAL_LEFT_X, PERSONAL_STATS_Y, 620, 248)
        g.font = font.deriveFont(Font.BOLD, 28f)
        g.color = ink
        g.drawString("资产概览", 86, PERSONAL_STATS_Y + 46)
        drawStat(g, font, "钱包金币", card.wallet, 86, PERSONAL_STATS_Y + 92, 210)
        drawStat(g, font, "签到收益", card.signEarnings, 366, PERSONAL_STATS_Y + 92, 210)
        drawStat(g, font, "银行收益", card.bankEarnings, 86, PERSONAL_STATS_Y + 160, 210)
        drawStat(g, font, "连续签到", card.signDays, 366, PERSONAL_STATS_Y + 160, 210)

        // 长日期比数值更容易挤压，所以单独占一行，并比指标组略微下沉。
        val lastSignY = PERSONAL_STATS_Y + 238
        g.font = font.deriveFont(Font.PLAIN, 19f)
        g.color = muted
        g.drawString("上次签到", 86, lastSignY)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, card.signTime, 380, 24f))
        g.color = ink
        g.drawString(card.signTime, 190, lastSignY)
    }

    private fun drawStat(g: Graphics2D, font: Font, label: String, value: String, x: Int, y: Int, maxWidth: Int) {
        g.font = font.deriveFont(Font.PLAIN, 21f)
        g.color = muted
        g.drawString(label, x, y)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, value, maxWidth, 31f))
        g.color = ink
        g.drawString(value, x, y + 34)
    }

    private fun drawPersonalBankPanel(g: Graphics2D, card: PersonalInfoCard, font: Font) {
        // 银行卡强调“银行名称 -> 金额”的对应关系，右侧金额右对齐方便扫读。
        drawPanel(g, PERSONAL_RIGHT_X, PERSONAL_TOP_Y, 516, 330)
        g.font = font.deriveFont(Font.BOLD, 28f)
        g.color = ink
        g.drawString("银行存款", 740, PERSONAL_TOP_Y + 46)
        g.font = font.deriveFont(Font.PLAIN, 20f)
        g.color = muted
        g.drawString("按银行分别展示", 740, PERSONAL_TOP_Y + 78)

        val deposits = if (card.bankDeposits.isEmpty()) listOf(BankDepositLine("暂无存款", "0")) else card.bankDeposits
        var y = PERSONAL_TOP_Y + 126
        deposits.take(5).forEachIndexed { index, line ->
            val color = listOf(green, blue, gold, red, Color(91, 108, 143))[index % 5]
            g.color = color
            g.fillRoundRect(742, y - 20, 10, 34, 10, 10)
            g.font = font.deriveFont(Font.BOLD, fitFontSize(g, line.bankName, 255, 24f))
            g.color = ink
            g.drawString(line.bankName, 766, y)
            g.font = font.deriveFont(Font.BOLD, fitFontSize(g, line.amount, 145, 26f))
            g.color = color
            val amountWidth = g.fontMetrics.stringWidth(line.amount)
            g.drawString(line.amount, 1182 - amountWidth, y)
            y += 48
        }
        if (card.bankDeposits.size > 5) {
            g.font = font.deriveFont(Font.PLAIN, 19f)
            g.color = muted
            g.drawString("另有 ${card.bankDeposits.size - 5} 个银行未展示", 742, PERSONAL_TOP_Y + 292)
        }
    }

    private fun drawPersonalQuote(g: Graphics2D, card: PersonalInfoCard, font: Font) {
        // 一言卡放在右下，作为轻量补充信息，避免干扰主要资产数据。
        drawPanel(g, PERSONAL_RIGHT_X, PERSONAL_QUOTE_Y, 516, 148)
        g.font = font.deriveFont(Font.BOLD, 26f)
        g.color = ink
        g.drawString("今日一句", 740, PERSONAL_QUOTE_Y + 46)
        g.font = font.deriveFont(Font.PLAIN, 22f)
        g.color = muted
        drawWrapped(g, card.hitokoto, 740, PERSONAL_QUOTE_Y + 86, 425, 28, 2)
        g.font = font.deriveFont(Font.PLAIN, 18f)
        g.color = Color(120, 130, 140)
        g.drawString(card.signature.take(34), 740, PERSONAL_QUOTE_Y + 128)
    }

    private fun drawFlatBackground(g: Graphics2D, width: Int, height: Int) {
        g.paint = GradientPaint(0f, 0f, bgTop, width.toFloat(), height.toFloat(), bgBottom)
        g.fillRect(0, 0, width, height)
        g.color = Color(255, 255, 255, 90)
        g.fillRect(0, 0, width, 8)
    }

    private fun drawPanel(g: Graphics2D, x: Int, y: Int, w: Int, h: Int) {
        g.color = Color(180, 190, 198, 42)
        g.fillRoundRect(x + 3, y + 5, w, h, PERSONAL_PANEL_RADIUS, PERSONAL_PANEL_RADIUS)
        g.color = panel
        g.fillRoundRect(x, y, w, h, PERSONAL_PANEL_RADIUS, PERSONAL_PANEL_RADIUS)
        g.color = line
        g.stroke = BasicStroke(1.2f)
        g.draw(RoundRectangle2D.Double(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble(), PERSONAL_PANEL_RADIUS.toDouble(), PERSONAL_PANEL_RADIUS.toDouble()))
    }

    private fun drawChip(g: Graphics2D, x: Int, y: Int, label: String, value: String, color: Color, font: Font) {
        g.color = Color(color.red, color.green, color.blue, 30)
        g.fillRoundRect(x, y, 138, 36, 16, 16)
        g.color = color
        g.fillRoundRect(x + 10, y + 11, 14, 14, 14, 14)
        g.font = font.deriveFont(Font.PLAIN, 15f)
        g.drawString(label, x + 30, y + 23)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, value, 74, 17f))
        g.color = ink
        g.drawString(value, x + 68, y + 24)
    }

    private fun drawFooter(g: Graphics2D, font: Font, x: Int, y: Int) {
        g.font = font.deriveFont(Font.PLAIN, 17f)
        g.color = Color(120, 130, 140)
        g.drawString("by Mirai & HuYanEconomy(壶言经济) v${EconomyBuildConstants.VERSION}", x, y)
    }

    private fun drawWrapped(g: Graphics2D, text: String, x: Int, y: Int, maxWidth: Int, lineHeight: Int, maxLines: Int) {
        var line = ""
        var currentY = y
        var lines = 0
        for (char in text) {
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
        if (line.isNotBlank() && lines < maxLines) g.drawString(line, x, currentY)
    }

    private fun fitFontSize(g: Graphics2D, text: String, maxWidth: Int, start: Float): Float {
        var size = start
        val base = g.font
        while (size > 12f) {
            val metrics = g.getFontMetrics(base.deriveFont(size))
            if (metrics.stringWidth(text) <= maxWidth) return size
            size -= 1f
        }
        return 12f
    }

    private fun resize(image: BufferedImage, width: Int, height: Int): BufferedImage {
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(output)
        g.drawImage(image, 0, 0, width, height, null)
        g.dispose()
        return output
    }

    private fun defaultFont(): Font = Font("Microsoft YaHei UI", Font.PLAIN, 24)

    private fun mainHelpSections(): List<HelpSection> = listOf(
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
        HelpSection(
            "经济与银行",
            listOf(
                HelpCommandItem("转账 @用户 金额", "向其他用户转账"),
                HelpCommandItem("存款 金额", "存入默认银行"),
                HelpCommandItem("存款 金额 银行", "存入指定银行"),
                HelpCommandItem("主存款 金额", "直达主银行存款"),
                HelpCommandItem("取款 金额 银行", "从指定银行取款"),
                HelpCommandItem("银行利率 / 富豪榜", "查询利率与排行")
            )
        ),
        HelpSection(
            "私人银行",
            listOf(
                HelpCommandItem("银行列表", "查看可用银行"),
                HelpCommandItem("银行创建 code 名称", "创建自己的银行"),
                HelpCommandItem("银行信息 [code]", "查看银行详情"),
                HelpCommandItem("银行评分 1-5 [内容]", "给银行评分"),
                HelpCommandItem("贷款 金额 [银行]", "申请银行贷款"),
                HelpCommandItem("还款 金额 [银行]", "偿还贷款")
            )
        ),
        HelpSection(
            "农场模块",
            listOf(
                HelpCommandItem("我的农场", "查看土地状态"),
                HelpCommandItem("农场商店 / 仓库", "种子与库存"),
                HelpCommandItem("购买种子 名称 [数量]", "购买作物种子"),
                HelpCommandItem("播种 / 收获 地块", "单块地操作"),
                HelpCommandItem("一键播种 / 收获", "批量农场操作"),
                HelpCommandItem("黑市 / 激活守护", "特殊农场功能")
            )
        ),
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
        HelpSection(
            "其他",
            listOf(
                HelpCommandItem("国卷 / 狐卷", "查看债券信息"),
                HelpCommandItem("国卷购买 金额", "银行购买债券"),
                HelpCommandItem("#fund bind QQ", "私聊绑定资助"),
                HelpCommandItem("#fund get code 金额", "领取资助金币"),
                HelpCommandItem("游戏帮助", "查看游戏指令图")
            )
        )
    )

    private fun gameHelpSections(): List<HelpSection> = listOf(
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
        HelpSection(
            "抢劫游戏",
            listOf(
                HelpCommandItem("抢劫 @用户", "按武力值判定成功率"),
                HelpCommandItem("抢劫榜", "查看抢劫排行"),
                HelpCommandItem("出院", "从医院恢复"),
                HelpCommandItem("开启 / 关闭 抢劫", "管理员开关")
            )
        ),
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
        HelpSection(
            "农场玩法",
            listOf(
                HelpCommandItem("我的农场", "查看农场状态"),
                HelpCommandItem("购买种子 名称 数量", "购买种子"),
                HelpCommandItem("播种 1,2,3 名称", "指定地块播种"),
                HelpCommandItem("收获 1,2,3", "指定地块收获"),
                HelpCommandItem("一键收获 / 卖出", "批量处理"),
                HelpCommandItem("帮浇水 @用户", "加速好友作物")
            )
        )
    )
}
