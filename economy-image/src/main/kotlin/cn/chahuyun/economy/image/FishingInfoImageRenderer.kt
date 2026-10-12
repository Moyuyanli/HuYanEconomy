package cn.chahuyun.economy.image

import cn.chahuyun.economy.image.model.FishingInfoCard
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage

/**
 * 钓鱼信息图渲染门面。
 *
 * 自包含绘制：优先使用底图（data/bottom），无底图时退化为纯色画布；
 * 文字样式统一使用自定义字体（data/font），保证与其它信息图一致。
 */
object FishingInfoImageRenderer {

    const val WIDTH: Int = 1280
    const val HEIGHT: Int = 720

    private const val MARGIN_X = 70
    private const val LABEL_X = 80
    private const val VALUE_X = 340
    private const val FIRST_LINE_Y = 250
    private const val LINE_HEIGHT = 54

    private val TITLE_COLOR = Color(0xFF, 0xD7, 0x00)
    private val LABEL_COLOR = Color(0xBD, 0xD7, 0xFF)
    private val TEXT_COLOR = Color(0xF5, 0xF7, 0xFA)
    private val CANVAS_FALLBACK = Color(0x1E, 0x28, 0x3B)

    @JvmStatic
    fun render(card: FishingInfoCard): BufferedImage {
        val canvas = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g = ImageUtil.getG2d(canvas)
        try {
            val bottom = ImageManager.getNextBottomShared()
            if (bottom != null) {
                g.drawImage(bottom, 0, 0, WIDTH, HEIGHT, null)
            } else {
                g.color = CANVAS_FALLBACK
                g.fillRect(0, 0, WIDTH, HEIGHT)
            }

            val font = ImageManager.getCustomFont()

            g.color = TITLE_COLOR
            g.font = font.deriveFont(Font.BOLD, 46f)
            g.drawString("钓鱼信息", MARGIN_X, 120)

            g.color = TEXT_COLOR
            g.font = font.deriveFont(Font.PLAIN, 30f)
            g.drawString(card.owner, MARGIN_X, 172)

            g.font = font.deriveFont(Font.PLAIN, 28f)
            var y = FIRST_LINE_Y
            for (line in card.lines) {
                g.color = LABEL_COLOR
                g.drawString(line.label, LABEL_X, y)
                g.color = TEXT_COLOR
                g.drawString(line.value, VALUE_X, y)
                y += LINE_HEIGHT
            }

            y += 20
            g.color = TITLE_COLOR
            g.font = font.deriveFont(Font.BOLD, 30f)
            g.drawString("最大渔获", LABEL_X, y)

            g.color = TEXT_COLOR
            g.font = font.deriveFont(Font.PLAIN, 28f)
            g.drawString(card.biggestFish, LABEL_X, y + 46)

            g.color = LABEL_COLOR
            g.font = font.deriveFont(Font.PLAIN, 24f)
            g.drawString(card.biggestFishDetail, LABEL_X, y + 88)
        } finally {
            g.dispose()
        }
        return ImageUtil.makeRoundedCorner(canvas, 30)
    }
}
