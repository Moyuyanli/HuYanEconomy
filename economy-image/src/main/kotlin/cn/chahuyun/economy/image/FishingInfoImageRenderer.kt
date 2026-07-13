package cn.chahuyun.economy.image

import cn.chahuyun.economy.image.model.FishingInfoCard
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

object FishingInfoImageRenderer {
    private const val WIDTH = 1280
    private const val HEIGHT = 720

    private val frameCache = ConcurrentHashMap<String, BufferedImage>()

    private val bgTop = Color(240, 248, 250)
    private val bgBottom = Color(225, 235, 239)
    private val ink = Color(34, 43, 50)
    private val muted = Color(94, 108, 118)
    private val panel = Color(255, 255, 255, 218)
    private val line = Color(182, 201, 211)
    private val teal = Color(45, 137, 139)
    private val blue = Color(58, 112, 170)
    private val gold = Color(198, 143, 55)
    private val red = Color(181, 83, 77)

    @JvmStatic
    fun render(card: FishingInfoCard): BufferedImage {
        val font = ImageManager.getCustomFont()
        val image = copy(frameFor(font))
        val g = ImageUtil.getG2d(image)

        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, card.owner, 560, 46f))
        g.color = ink
        g.drawString(card.owner, 72, 94)
        g.font = font.deriveFont(Font.PLAIN, 22f)
        g.color = muted
        g.drawString("钓鱼信息", 76, 132)

        drawMetric(g, font, "鱼竿等级", card.rodLevel, 86, 228, 250, teal)
        drawMetric(g, font, "最多钓鱼鱼塘", card.maxPond, 386, 228, 340, blue)
        drawMetric(g, font, "历史钓鱼次数", card.historyCount, 786, 228, 180, gold)
        drawMetric(g, font, "上鱼次数", card.successCount, 1010, 228, 160, red)

        drawBiggestFish(g, font, card)
        drawCurrentPond(g, font, card)

        g.dispose()
        return image
    }

    private fun frameFor(font: Font): BufferedImage {
        val key = "${font.family}-${font.style}-${font.size}"
        return frameCache.computeIfAbsent(key) {
            val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
            val g = ImageUtil.getG2d(image)
            g.paint = GradientPaint(0f, 0f, bgTop, WIDTH.toFloat(), HEIGHT.toFloat(), bgBottom)
            g.fillRect(0, 0, WIDTH, HEIGHT)
            g.color = Color(255, 255, 255, 72)
            g.fillOval(916, -160, 420, 420)
            g.color = Color(53, 134, 143, 28)
            g.fillOval(-130, 470, 340, 340)
            drawPanel(g, 56, 54, 1168, 116)
            drawPanel(g, 56, 194, 1168, 114)
            drawPanel(g, 56, 344, 548, 268)
            drawPanel(g, 636, 344, 588, 268)
            drawFooter(g, font)
            g.dispose()
            image
        }
    }

    private fun drawMetric(
        g: Graphics2D,
        font: Font,
        label: String,
        value: String,
        x: Int,
        y: Int,
        maxWidth: Int,
        color: Color,
    ) {
        g.color = Color(color.red, color.green, color.blue, 32)
        g.fillRoundRect(x - 16, y - 48, maxWidth + 24, 78, 16, 16)
        g.color = color
        g.fillRoundRect(x - 2, y - 30, 10, 44, 10, 10)
        g.font = font.deriveFont(Font.PLAIN, 18f)
        g.color = muted
        g.drawString(label, x + 22, y - 16)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, value, maxWidth - 20, 28f))
        g.color = ink
        g.drawString(value, x + 22, y + 18)
    }

    private fun drawBiggestFish(g: Graphics2D, font: Font, card: FishingInfoCard) {
        g.font = font.deriveFont(Font.BOLD, 30f)
        g.color = ink
        g.drawString("最大的鱼", 90, 398)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, card.biggestFish, 440, 42f))
        g.color = teal
        g.drawString(card.biggestFish, 90, 468)
        drawDetailSegments(g, font, card.biggestFishDetail, 90, 514, 480, 4)
    }

    private fun drawCurrentPond(g: Graphics2D, font: Font, card: FishingInfoCard) {
        g.font = font.deriveFont(Font.BOLD, 30f)
        g.color = ink
        g.drawString("当前鱼塘", 670, 398)
        g.font = font.deriveFont(Font.BOLD, fitFontSize(g, card.currentPond, 500, 42f))
        g.color = blue
        g.drawString(card.currentPond, 670, 468)
        drawDetailSegments(g, font, card.currentPondDetail, 670, 514, 510, 4)
    }

    private fun drawPanel(g: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        g.color = Color(36, 52, 66, 20)
        g.fillRoundRect(x + 3, y + 5, width, height, 18, 18)
        g.color = panel
        g.fillRoundRect(x, y, width, height, 18, 18)
        g.color = line
        g.stroke = BasicStroke(1.1f)
        g.draw(RoundRectangle2D.Double(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), 18.0, 18.0))
    }

    private fun drawFooter(g: Graphics2D, font: Font) {
        val text = "by Mirai + Overflow & HuYanEconomy"
        g.font = font.deriveFont(Font.PLAIN, 17f)
        g.color = Color(120, 130, 140)
        g.drawString(text, WIDTH - 42 - g.fontMetrics.stringWidth(text), HEIGHT - 28)
    }

    private fun drawDetailSegments(
        g: Graphics2D,
        font: Font,
        text: String,
        x: Int,
        y: Int,
        maxWidth: Int,
        maxLines: Int,
    ) {
        g.font = font.deriveFont(Font.PLAIN, 21f)
        g.color = muted

        val segments = text
            .split(" / ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(text) }

        var currentY = y
        var drawn = 0
        for (segment in segments) {
            if (drawn >= maxLines) return
            g.drawString(fitText(g, segment, maxWidth), x, currentY)
            currentY += 30
            drawn += 1
        }
    }

    private fun fitText(g: Graphics2D, text: String, maxWidth: Int): String {
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
