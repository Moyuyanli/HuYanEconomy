package cn.chahuyun.economy.utils

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Mirai 图片消息发送工具。
 *
 * 图片模块只负责输出 BufferedImage；上传和发送仍由当前 core/main 的 Mirai 侧流程完成。
 */
object ImageMessageUtil {
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
        ByteArrayInputStream(bytes).toExternalResource().use { resource ->
            subject.sendMessage(subject.uploadImage(resource))
        }
    }

    suspend fun uploadImageFromUrl(subject: Contact, imageUrl: String): Image {
        URL(imageUrl).openConnection().getInputStream().use { input ->
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
}
