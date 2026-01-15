package cn.chahuyun.economy.utils

import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage

object ImageUtil {

    /**
     * 圆角处理
     */
    @JvmStatic
    fun makeRoundedCorner(image: BufferedImage, cornerRadius: Int): BufferedImage {
        val w = image.width
        val h = image.height
        var output = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        var g2 = output.createGraphics()

        output = g2.deviceConfiguration.createCompatibleImage(w, h, Transparency.TRANSLUCENT)
        g2.dispose()
        g2 = output.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius)
        g2.composite = AlphaComposite.SrcIn

        g2.drawImage(image, 0, 0, w, h, null)
        g2.dispose()

        return output
    }

    /**
     * 按区域写入文字，自动换行
     */
    @JvmStatic
    fun drawString(text: String, x: Int, y: Int, maxWidth: Int, g2d: Graphics2D) {
        var areaX = x.toFloat()
        var areaY = y.toFloat()
        val max = x + maxWidth

        val frc = g2d.fontRenderContext
        val font: Font = g2d.font

        for (line in text.split("\n")) {
            if (line.isBlank()) {
                continue
            }

            val layout = TextLayout(line, font, frc)
            val bounds: Rectangle2D = layout.bounds

            if (bounds.width > maxWidth) {
                for (word in line.toCharArray()) {
                    val wordLayout = TextLayout(word.toString(), font, frc)
                    val wordBounds = wordLayout.bounds

                    if (areaX + wordBounds.width > max) {
                        areaX = x.toFloat()
                        areaY += bounds.height.toFloat()
                    }

                    wordLayout.draw(g2d, areaX, areaY)
                    areaX += wordBounds.width.toFloat() + 4f
                }
            } else {
                layout.draw(g2d, areaX, areaY)
                areaY += bounds.height.toFloat()
            }

            areaX = x.toFloat()
        }
    }

    /**
     * 设置渐变文字
     */
    @JvmStatic
    fun drawStringGradient(text: String, x: Int, y: Int, sColor: Color, eColor: Color, g2d: Graphics2D) {
        val frc: FontRenderContext = g2d.fontRenderContext
        val textWidth = g2d.font.getStringBounds(text, frc).width.toFloat()
        val gradient = GradientPaint(x.toFloat(), y.toFloat(), sColor, x + textWidth, y.toFloat(), eColor)
        g2d.paint = gradient
        g2d.drawString(text, x.toFloat(), y.toFloat())
    }

    /**
     * 规范g2d
     */
    @JvmStatic
    fun getG2d(bufferedImage: BufferedImage): Graphics2D {
        val g2d = bufferedImage.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        return g2d
    }

    /**
     * 十六进制颜色
     */
    @JvmStatic
    fun hexColor(color: String): Color {
        if (color.isNotEmpty()) {
            val string = if (color.length == 7) color.substring(1) else color
            val r = string.substring(0, 2).toInt(16)
            val g = string.substring(2, 4).toInt(16)
            val b = string.substring(4, 6).toInt(16)
            return Color(r, g, b)
        }
        throw IllegalArgumentException("构建颜色错误!")
    }

    /**
     * 将color换成16进制的颜色，不带#
     */
    @JvmStatic
    fun colorHex(color: Color): String {
        var red = Integer.toHexString(color.red)
        var green = Integer.toHexString(color.green)
        var blue = Integer.toHexString(color.blue)

        if (red.length == 1) red = "0$red"
        if (green.length == 1) green = "0$green"
        if (blue.length == 1) blue = "0$blue"

        return red + green + blue
    }
}
