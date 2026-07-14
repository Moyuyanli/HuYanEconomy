package cn.chahuyun.economy.image

import cn.chahuyun.economy.image.model.PersonalInfoCard
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * 个人信息图渲染门面。
 *
 * Core 负责组装 PersonalInfoCard；image 负责头像读取、底图选择和最终绘制。
 */
object PersonalInfoImageRenderer {
    private const val AVATAR_CACHE_TTL_MILLIS = 10 * 60 * 1000L
    private const val AVATAR_CACHE_MAX_SIZE = 256
    private const val NETWORK_TIMEOUT_MILLIS = 5_000

    private val avatarCache = ConcurrentHashMap<String, CachedAvatar>()

    @JvmStatic
    @Throws(IOException::class)
    fun render(card: PersonalInfoCard, avatarUrl: String): BufferedImage {
        val avatar = loadAvatar(avatarUrl)
        return EconomyImageRenderer.renderPersonalInfo(
            card = card,
            avatar = avatar,
            background = ImageManager.getNextBottomShared(),
            font = ImageManager.getCustomFont()
        )
    }

    private fun loadAvatar(avatarUrl: String): BufferedImage {
        val now = System.currentTimeMillis()
        avatarCache[avatarUrl]?.takeIf { it.expiresAt > now }?.let {
            return it.image
        }

        val connection = URL(avatarUrl).openConnection().apply {
            connectTimeout = NETWORK_TIMEOUT_MILLIS
            readTimeout = NETWORK_TIMEOUT_MILLIS
        }
        val image = connection.getInputStream().use { ImageIO.read(it) }
            ?: throw IOException("头像读取失败: $avatarUrl")
        if (avatarCache.size >= AVATAR_CACHE_MAX_SIZE) {
            trimAvatarCache(now)
        }
        avatarCache[avatarUrl] = CachedAvatar(image, now + AVATAR_CACHE_TTL_MILLIS)
        return image
    }

    private fun trimAvatarCache(now: Long) {
        avatarCache.entries.removeIf { it.value.expiresAt <= now }
        if (avatarCache.size < AVATAR_CACHE_MAX_SIZE) return
        avatarCache.keys.take((AVATAR_CACHE_MAX_SIZE / 4).coerceAtLeast(1)).forEach {
            avatarCache.remove(it)
        }
    }

    private data class CachedAvatar(
        val image: BufferedImage,
        val expiresAt: Long,
    )
}
