package cn.chahuyun.economy.utils

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO

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
}
