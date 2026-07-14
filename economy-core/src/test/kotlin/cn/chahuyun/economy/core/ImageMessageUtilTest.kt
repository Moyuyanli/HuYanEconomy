package cn.chahuyun.economy.core

import cn.chahuyun.economy.utils.ImageMessageUtil
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageMessageUtilTest {

    @Test
    fun `size limited encoding stays below requested byte count`() {
        val image = BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB)
        val random = Random(20260713)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                image.setRGB(x, y, random.nextInt())
            }
        }

        val maxBytes = 128 * 1024
        val bytes = ImageMessageUtil.toSizeLimitedImageBytes(image, maxBytes)

        assertTrue(bytes.size <= maxBytes, "encoded image was ${bytes.size} bytes")
        assertNotNull(ImageIO.read(ByteArrayInputStream(bytes)))
    }
}
