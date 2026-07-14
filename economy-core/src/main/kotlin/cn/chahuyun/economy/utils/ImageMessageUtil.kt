package cn.chahuyun.economy.utils

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Mirai 图片消息发送工具。
 *
 * 图片模块只负责输出 BufferedImage；上传和发送仍由当前 core/main 的 Mirai 侧流程完成。
 */
object ImageMessageUtil {
    private const val NETWORK_TIMEOUT_MILLIS = 5_000
    const val DEFAULT_MAX_IMAGE_BYTES = 1024 * 1024

    private val WEBP_QUALITY_STEPS = listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f)

    init {
        ImageIO.setUseCache(false)
    }

    suspend fun sendImage(subject: Contact, image: BufferedImage) {
        sendPngBytes(subject, toPngBytes(image))
    }

    suspend fun sendQuotedImage(subject: Contact, quote: MessageChain, image: BufferedImage) {
        ByteArrayInputStream(toPngBytes(image)).toExternalResource().use { resource ->
            subject.sendMessage(MessageUtil.quoteReply(quote).append(subject.uploadImage(resource)).build())
        }
    }

    suspend fun sendQuotedWebpImage(subject: Contact, quote: MessageChain, image: BufferedImage, quality: Float = 0.72f) {
        ByteArrayInputStream(toWebpBytesOrPng(image, quality)).toExternalResource().use { resource ->
            subject.sendMessage(MessageUtil.quoteReply(quote).append(subject.uploadImage(resource)).build())
        }
    }

    suspend fun sendPngBytes(subject: Contact, bytes: ByteArray) {
        sendImageBytes(subject, bytes)
    }

    suspend fun sendImageBytes(subject: Contact, bytes: ByteArray) {
        ByteArrayInputStream(bytes).toExternalResource().use { resource ->
            subject.sendMessage(subject.uploadImage(resource))
        }
    }

    suspend fun uploadImageFromUrl(subject: Contact, imageUrl: String): Image {
        val connection = URL(imageUrl).openConnection().apply {
            connectTimeout = NETWORK_TIMEOUT_MILLIS
            readTimeout = NETWORK_TIMEOUT_MILLIS
        }
        connection.getInputStream().use { input ->
            input.toExternalResource().use { resource ->
                return subject.uploadImage(resource)
            }
        }
    }

    fun toPngBytes(image: BufferedImage): ByteArray {
        val stream = ByteArrayOutputStream(image.width * image.height / 2)
        ImageIO.write(image, "png", stream)
        return stream.toByteArray()
    }

    fun toWebpBytesOrPng(image: BufferedImage, quality: Float = 0.72f): ByteArray =
        runCatching { toWebpBytes(image, quality) }.getOrNull() ?: toPngBytes(image)

    fun toSizeLimitedImageBytes(
        image: BufferedImage,
        maxBytes: Int = DEFAULT_MAX_IMAGE_BYTES,
    ): ByteArray {
        require(maxBytes > 0) { "maxBytes must be greater than 0" }

        var current = image
        while (true) {
            var smallestWebp: ByteArray? = null
            for (quality in WEBP_QUALITY_STEPS) {
                val encoded = runCatching { toWebpBytes(current, quality) }.getOrNull() ?: break
                smallestWebp = encoded
                if (encoded.size <= maxBytes) return encoded
            }

            val smallest = smallestWebp ?: toPngBytes(current)
            if (smallest.size <= maxBytes || current.width == 1 && current.height == 1) return smallest

            val targetScale = sqrt(maxBytes.toDouble() / smallest.size.toDouble()) * 0.9
            val scale = targetScale.coerceIn(0.1, 0.9)
            val width = floor(current.width * scale).toInt().coerceIn(1, (current.width - 1).coerceAtLeast(1))
            val height = floor(current.height * scale).toInt().coerceIn(1, (current.height - 1).coerceAtLeast(1))
            current = resize(current, width, height)
        }
    }

    private fun toWebpBytes(image: BufferedImage, quality: Float): ByteArray? {
        val writers = ImageIO.getImageWritersByFormatName("webp")
        if (!writers.hasNext()) return null

        val writer = writers.next()
        val stream = ByteArrayOutputStream(image.width * image.height / 5)
        try {
            ImageIO.createImageOutputStream(stream).use { output ->
                writer.output = output
                val param = writer.defaultWriteParam
                if (param.canWriteCompressed()) {
                    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    param.compressionQuality = quality.coerceIn(0.1f, 1.0f)
                }
                writer.write(null, IIOImage(toRgbImage(image), null, null), param)
            }
        } finally {
            writer.dispose()
        }
        return stream.toByteArray()
    }

    private fun toRgbImage(image: BufferedImage): BufferedImage {
        if (image.type == BufferedImage.TYPE_INT_RGB) return image
        val output = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val g = output.createGraphics()
        g.drawImage(image, 0, 0, null)
        g.dispose()
        return output
    }

    private fun resize(image: BufferedImage, width: Int, height: Int): BufferedImage {
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = output.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(image, 0, 0, width, height, null)
        g.dispose()
        return output
    }
}
