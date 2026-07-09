package cn.chahuyun.economy.image

import cn.chahuyun.economy.EconomyBuildConstants
import cn.chahuyun.economy.image.model.FarmDetailCard
import cn.chahuyun.economy.image.model.FarmPlotDetailLine
import cn.chahuyun.economy.image.model.FarmPlotDetailStatus
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

object FarmDetailImageRenderer {
    private const val WIDTH = 1280
    private const val HEIGHT = 960
    private const val PLOT_COLUMNS = 3
    private const val PLOT_WIDTH = 360
    private const val PLOT_HEIGHT = 102
    private const val PLOT_GAP_X = 34
    private const val PLOT_GAP_Y = 22
    private const val PLOT_START_X = 84
    private const val PLOT_START_Y = 306

    private val frameCache = ConcurrentHashMap<String, BufferedImage>()

    private val bgTop = Color(244, 249, 244)
    private val bgBottom = Color(225, 236, 229)
    private val ink = Color(39, 49, 45)
    private val muted = Color(99, 112, 106)
    private val line = Color(190, 207, 196)
    private val panel = Color(255, 255, 255, 218)
    private val green = Color(64, 142, 95)
    private val gold = Color(199, 145, 57)
    private val blue = Color(64, 121, 163)
    private val red = Color(185, 84, 74)
    private val gray = Color(122, 132, 128)

    @JvmStatic
    fun render(card: FarmDetailCard): BufferedImage {
        val font = ImageManager.getCustomFont()
        val base = frameFor(font)
        val image = copy(base)
        val g = ImageUtil.getG2d(image)
        drawSummary(g, card, font)
        drawPlots(g, card.plots, font)
        drawFooter(g, font)
        g.dispose()
        return image
    }

    private fun frameFor(font: Font): BufferedImage {
        val key = "${font.family}-${font.style}-${font.size}"
        return frameCache.computeIfAbsent(key) { createFrame(font) }
    }

    private fun createFrame(font: Font): BufferedImage {
        val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(image)

        g.paint = GradientPaint(0f, 0f, bgTop, WIDTH.toFloat(), HEIGHT.toFloat(), bgBottom)
        g.fillRect(0, 0, WIDTH, HEIGHT)
        g.color = Color(255, 255, 255, 72)
        g.fillOval(926, -160, 430, 430)
        g.color = Color(105, 150, 122, 24)
        g.fillOval(-130, 620, 360, 360)

        drawPanel(g, 56, 54, 1168, 204)
        drawPanel(g, 56, 282, 1168, 618)

        g.font = font.deriveFont(Font.BOLD, 48f)
        g.color = ink
        g.drawString("农场详情", 92, 128)

        g.font = font.deriveFont(Font.PLAIN, 22f)
        g.color = muted
        g.drawString("地块、作物、成熟状态与今日浇水状态", 96, 168)

        g.font = font.deriveFont(Font.BOLD, 26f)
        g.color = ink
        g.drawString("地块明细", 92, 336)

        for (index in 0 until 18) {
            val col = index % PLOT_COLUMNS
            val row = index / PLOT_COLUMNS
            val x = PLOT_START_X + col * (PLOT_WIDTH + PLOT_GAP_X)
            val y = PLOT_START_Y + row * (PLOT_HEIGHT + PLOT_GAP_Y)
            g.color = Color(255, 255, 255, 148)
            g.fillRoundRect(x, y, PLOT_WIDTH, PLOT_HEIGHT, 18, 18)
            g.color = Color(188, 204, 195, 170)
            g.stroke = BasicStroke(1.1f)
            g.draw(RoundRectangle2D.Double(x.toDouble(), y.toDouble(), PLOT_WIDTH.toDouble(), PLOT_HEIGHT.toDouble(), 18.0, 18.0))
        }

        g.dispose()
        return image
    }

    private fun drawSummary(g: Graphics2D, card: FarmDetailCard, font: Font) {
        drawStat(g, font, "农场主", card.owner, 92, 218, 210, green)
        drawStat(g, font, "等级", "Lv.${card.level}", 318, 218, 120, gold)
        drawStat(g, font, "地块", "${card.unlockedPlots}/${card.totalPlots}", 492, 218, 120, blue)
        drawStat(g, font, "已种植", "${card.plantedPlots}", 664, 218, 100, green)
        drawStat(g, font, "可收获", "${card.readyPlots}", 824, 218, 100, red)

        drawChip(g, 962, 96, "守护", card.shieldText, blue, font, 218)
        drawChip(g, 962, 152, "今日帮浇水", card.waterText, green, font, 218)

        g.font = font.deriveFont(Font.PLAIN, 18f)
        g.color = muted
        g.drawString(card.waterHint, 964, 234)
    }

    private fun drawPlots(g: Graphics2D, plots: List<FarmPlotDetailLine>, font: Font) {
        plots.take(18).forEachIndexed { index, plot ->
            val col = index % PLOT_COLUMNS
            val row = index / PLOT_COLUMNS
            val x = PLOT_START_X + col * (PLOT_WIDTH + PLOT_GAP_X)
            val y = PLOT_START_Y + row * (PLOT_HEIGHT + PLOT_GAP_Y)
            val color = colorFor(plot.status)

            g.color = Color(color.red, color.green, color.blue, 38)
            g.fillRoundRect(x + 1, y + 1, PLOT_WIDTH - 2, PLOT_HEIGHT - 2, 18, 18)
            g.color = color
            g.fillRoundRect(x + 18, y + 18, 10, 64, 10, 10)

            g.font = font.deriveFont(Font.BOLD, 23f)
            g.color = ink
            g.drawString("${plot.plotNo.toString().padStart(2, '0')}  ${plot.title}", x + 44, y + 34)

            g.font = font.deriveFont(Font.PLAIN, 17f)
            g.color = muted
            g.drawString(fit(plot.subtitle, g, 198), x + 44, y + 62)

            g.font = font.deriveFont(Font.BOLD, 18f)
            g.color = color
            val statusWidth = g.fontMetrics.stringWidth(plot.statusText)
            g.drawString(plot.statusText, x + PLOT_WIDTH - 26 - statusWidth, y + 34)

            g.font = font.deriveFont(Font.PLAIN, 16f)
            g.color = muted
            val progressWidth = g.fontMetrics.stringWidth(plot.progressText)
            g.drawString(plot.progressText, x + PLOT_WIDTH - 26 - progressWidth, y + 74)
        }
    }

    private fun drawStat(g: Graphics2D, font: Font, label: String, value: String, x: Int, y: Int, maxWidth: Int, color: Color) {
        g.font = font.deriveFont(Font.PLAIN, 18f)
        g.color = muted
        g.drawString(label, x, y - 34)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, value, maxWidth, 28f))
        g.color = color
        g.drawString(value, x, y)
    }

    private fun drawChip(g: Graphics2D, x: Int, y: Int, label: String, value: String, color: Color, font: Font, width: Int) {
        g.color = Color(color.red, color.green, color.blue, 30)
        g.fillRoundRect(x, y, width, 38, 18, 18)
        g.color = color
        g.fillRoundRect(x + 13, y + 12, 14, 14, 14, 14)
        g.font = font.deriveFont(Font.PLAIN, 16f)
        g.drawString(label, x + 34, y + 25)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, value, width - 118, 18f))
        g.color = ink
        g.drawString(value, x + 118, y + 25)
    }

    private fun drawPanel(g: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        g.color = Color(35, 58, 44, 20)
        g.fillRoundRect(x + 3, y + 5, width, height, 22, 22)
        g.color = panel
        g.fillRoundRect(x, y, width, height, 22, 22)
        g.color = line
        g.stroke = BasicStroke(1.2f)
        g.draw(RoundRectangle2D.Double(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), 22.0, 22.0))
    }

    private fun drawFooter(g: Graphics2D, font: Font) {
        val text = "by Mirai + Overflow & HuYanEconomy(壶言经济) v${EconomyBuildConstants.VERSION}"
        g.font = font.deriveFont(Font.PLAIN, 16f)
        g.color = Color(112, 126, 118)
        g.drawString(text, WIDTH - 48 - g.fontMetrics.stringWidth(text), HEIGHT - 28)
    }

    private fun colorFor(status: FarmPlotDetailStatus): Color =
        when (status) {
            FarmPlotDetailStatus.LOCKED -> gray
            FarmPlotDetailStatus.EMPTY -> blue
            FarmPlotDetailStatus.GROWING -> green
            FarmPlotDetailStatus.READY -> red
        }

    private fun fit(text: String, g: Graphics2D, maxWidth: Int): String {
        if (g.fontMetrics.stringWidth(text) <= maxWidth) return text
        var value = text
        while (value.length > 1 && g.fontMetrics.stringWidth("$value...") > maxWidth) {
            value = value.dropLast(1)
        }
        return "$value..."
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

    private fun copy(source: BufferedImage): BufferedImage {
        val output = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        val g = output.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g.drawImage(source, 0, 0, null)
        g.dispose()
        return output
    }
}
