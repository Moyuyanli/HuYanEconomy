package cn.chahuyun.economy.service

import cn.chahuyun.economy.image.EconomyImageRenderer
import cn.chahuyun.economy.image.ImageManager
import cn.chahuyun.economy.utils.ImageMessageUtil
import cn.chahuyun.economy.utils.Log
import net.mamoe.mirai.event.events.GroupMessageEvent

object HelpImageService {
    @Volatile
    private var mainHelpCache: ByteArray? = null

    @Volatile
    private var gameHelpCache: ByteArray? = null

    suspend fun sendMainHelp(event: GroupMessageEvent) {
        sendHelpImage(event, mainHelpCache ?: synchronized(this) {
            mainHelpCache ?: ImageMessageUtil.toPngBytes(
                EconomyImageRenderer.renderMainHelp(ImageManager.getCustomFont())
            ).also {
                mainHelpCache = it
            }
        })
    }

    suspend fun sendGameHelp(event: GroupMessageEvent) {
        sendHelpImage(event, gameHelpCache ?: synchronized(this) {
            gameHelpCache ?: ImageMessageUtil.toPngBytes(
                EconomyImageRenderer.renderGameHelp(ImageManager.getCustomFont())
            ).also {
                gameHelpCache = it
            }
        })
    }

    private suspend fun sendHelpImage(event: GroupMessageEvent, bytes: ByteArray) {
        try {
            ImageMessageUtil.sendPngBytes(event.subject, bytes)
        } catch (e: Exception) {
            Log.error("帮助图片生成或发送失败", e)
            event.subject.sendMessage("帮助图片生成失败，请稍后再试。")
        }
    }
}
