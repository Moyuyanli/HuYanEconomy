package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.economy.plugin.ImageManager
import cn.chahuyun.economy.utils.EconomyImageRenderer
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@EventComponent
class HelpAction {

    @MessageAuthorize(["help", "帮助"])
    suspend fun help(event: GroupMessageEvent) {
        sendHelpImage(event, EconomyImageRenderer.renderMainHelp(ImageManager.getCustomFont()))
    }

    @MessageAuthorize(["gameHelp", "游戏帮助"])
    suspend fun gameHelp(event: GroupMessageEvent) {
        sendHelpImage(event, EconomyImageRenderer.renderGameHelp(ImageManager.getCustomFont()))
    }

    private suspend fun sendHelpImage(event: GroupMessageEvent, image: BufferedImage) {
        try {
            val stream = ByteArrayOutputStream()
            ImageIO.write(image, "png", stream)
            ByteArrayInputStream(stream.toByteArray()).toExternalResource().use { resource ->
                event.subject.sendMessage(event.subject.uploadImage(resource))
            }
        } catch (e: Exception) {
            Log.error("帮助图片生成或发送失败", e)
            event.subject.sendMessage("帮助图片生成失败，请稍后再试。")
        }
    }
}
