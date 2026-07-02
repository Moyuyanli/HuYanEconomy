package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.utils.EconomyImageRenderer
import cn.chahuyun.economy.utils.FormatUtil
import cn.chahuyun.economy.utils.ImageUtil
import cn.chahuyun.economy.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.awt.Font
import java.awt.FontFormatException
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingUtilities

/**
 * 底图管理（Kotlin 实现）
 *
 * - 初始化阶段并发加载 data/bottom 下的图片并写入缓存
 * - 保持 Java 侧静态调用方式：ImageManager.init / getNextBottom / getCustomFont
 */
object ImageManager {

    @Volatile
    private var bufferedImages: List<BufferedImage> = emptyList()

    private val next = AtomicInteger(1)

    @Volatile
    private var customFont: Font = Font("宋体", Font.PLAIN, 24)

    @JvmStatic
    fun getCustomFont(): Font = customFont

    @JvmStatic
    @Throws(IOException::class, FontFormatException::class)
    fun init(instance: HuYanEconomy) {
        Log.info("开始加载字体...")

        val path = instance.dataFolderPath
        val fontDir = path.resolve("font").toFile()
        customFont = loadFontOrDefault(fontDir)

        val bottomDir = path.resolve("bottom").toFile()
        if (!bottomDir.exists()) return

        val files = bottomDir.listFiles()?.toList().orEmpty()
        val totalFiles = files.size
        if (totalFiles == 0) {
            bufferedImages = emptyList()
            Log.info("自定义图片和字体加载完成!")
            return
        }

        // 输出初始进度
        Log.info("开始加载自定义图片...")

        // 每 20% 打一次进度，保留原来的“分阶段”体验
        val step = (totalFiles / 5).let { if (it == 0) 1 else it }
        val completed = AtomicInteger(0)

        val loaded = runBlocking {
            files.map { file ->
                async(Dispatchers.IO) {
                    try {
                        loadOneBottomImage(file)
                    } finally {
                        val done = completed.incrementAndGet()
                        if (done % step == 0 || done == totalFiles) {
                            val percentage = done.toDouble() / totalFiles.toDouble() * 100.0
                            Log.info("处理进度: ${FormatUtil.fixed(percentage, 2)}%")
                        }
                    }
                }
            }.awaitAll()
        }.filterNotNull()

        bufferedImages = loaded
        Log.info("自定义图片和字体加载完成!")
    }

    @JvmStatic
    fun view(image: BufferedImage) {
        SwingUtilities.invokeLater {
            val frame = JFrame("Image Display")
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.setSize(image.width + 50, image.height + 50)
            frame.add(JLabel(ImageIcon(image)), BorderLayout.CENTER)
            frame.isVisible = true
        }
    }

    /**
     * 获取下一个底图（深拷贝，避免外部绘制污染缓存）
     */
    @JvmStatic
    fun getNextBottom(): BufferedImage? {
        val list = bufferedImages
        if (list.isEmpty()) return null

        val idx = (next.getAndIncrement() % list.size).let { if (it < 0) -it else it }
        val bufferedImage = list[idx]
        return BufferedImage(
            bufferedImage.colorModel,
            bufferedImage.copyData(null),
            bufferedImage.colorModel.isAlphaPremultiplied,
            null
        )
    }

    private fun loadFontOrDefault(fontDir: File): Font {
        if (!fontDir.exists()) {
            return Font("宋体", Font.PLAIN, 24)
        }
        val files = fontDir.listFiles()
        if (files.isNullOrEmpty()) {
            return Font("宋体", Font.PLAIN, 24)
        }
        // 兼容旧逻辑：取目录下第一个字体文件
        val fontFile = files[0]
        return try {
            Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(24f)
        } catch (_: Exception) {
            Font("宋体", Font.PLAIN, 24)
        }
    }

    private fun loadOneBottomImage(file: File): BufferedImage? {
        if (!file.isFile) return null
        val background = try {
            ImageIO.read(file)
        } catch (e: IOException) {
            Log.error("读取文件 ${file.name} 出错: ${e.message}")
            null
        }
        if (background == null) return null
        return drawBottom(background)
    }

    /**
     * 生成一个“已合成底 + 圆角”的底图缓存项
     */
    private fun drawBottom(background: BufferedImage): BufferedImage {
        val canvas = BufferedImage(EconomyImageRenderer.PERSONAL_WIDTH, EconomyImageRenderer.PERSONAL_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g2d: Graphics2D = ImageUtil.getG2d(canvas)

        val scaleWidth = EconomyImageRenderer.PERSONAL_WIDTH.toDouble() / background.width.toDouble()
        val scaleHeight = EconomyImageRenderer.PERSONAL_HEIGHT.toDouble() / background.height.toDouble()
        val scale = maxOf(scaleWidth, scaleHeight)

        val scaledWidth = (background.width * scale).toInt()
        val scaledHeight = (background.height * scale).toInt()

        val x = (EconomyImageRenderer.PERSONAL_WIDTH - scaledWidth) / 2
        val y = (EconomyImageRenderer.PERSONAL_HEIGHT - scaledHeight) / 2

        g2d.drawImage(background, x, y, scaledWidth, scaledHeight, null)

        g2d.dispose()
        return ImageUtil.makeRoundedCorner(canvas, 30)
    }
}

