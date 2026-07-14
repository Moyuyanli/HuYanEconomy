package cn.chahuyun.economy.service

import cn.chahuyun.economy.image.EconomyImageRenderer
import cn.chahuyun.economy.image.ImageManager
import cn.chahuyun.economy.utils.ImageMessageUtil
import cn.chahuyun.economy.utils.Log
import kotlinx.coroutines.withContext
import net.mamoe.mirai.event.events.GroupMessageEvent

object HelpImageService {
    @Volatile
    private var mainHelpCache: ByteArray? = null

    @Volatile
    private var gameHelpCache: ByteArray? = null

    suspend fun sendMainHelp(event: GroupMessageEvent) {
        val bytes = withContext(EconomyAsyncService.coroutineDispatcher()) {
            mainHelpCache ?: synchronized(this@HelpImageService) {
                mainHelpCache ?: ImageMessageUtil.toPngBytes(
                    EconomyImageRenderer.renderMainHelp(ImageManager.getCustomFont())
                ).also {
                    mainHelpCache = it
                }
            }
        }
        sendHelpImage(event, bytes)
    }

    suspend fun sendGameHelp(event: GroupMessageEvent) {
        val bytes = withContext(EconomyAsyncService.coroutineDispatcher()) {
            gameHelpCache ?: synchronized(this@HelpImageService) {
                gameHelpCache ?: ImageMessageUtil.toPngBytes(
                    EconomyImageRenderer.renderGameHelp(ImageManager.getCustomFont())
                ).also {
                    gameHelpCache = it
                }
            }
        }
        sendHelpImage(event, bytes)
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
