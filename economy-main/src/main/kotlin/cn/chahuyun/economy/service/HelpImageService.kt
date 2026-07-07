package cn.chahuyun.economy.service

import cn.chahuyun.economy.image.EconomyImageRenderer
import cn.chahuyun.economy.image.ImageManager
import cn.chahuyun.economy.utils.ImageMessageUtil
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.event.events.GroupMessageEvent
import java.awt.image.BufferedImage

object HelpImageService {

    suspend fun sendMainHelp(event: GroupMessageEvent) {
        sendHelpImage(event, EconomyImageRenderer.renderMainHelp(ImageManager.getCustomFont()))
    }

    suspend fun sendGameHelp(event: GroupMessageEvent) {
        sendHelpImage(event, EconomyImageRenderer.renderGameHelp(ImageManager.getCustomFont()))
    }

    private suspend fun sendHelpImage(event: GroupMessageEvent, image: BufferedImage) {
        try {
            ImageMessageUtil.sendImage(event.subject, image)
        } catch (e: Exception) {
            Log.error("帮助图片生成或发送失败", e)
            event.subject.sendMessage("帮助图片生成失败，请稍后再试。")
        }
    }
}
