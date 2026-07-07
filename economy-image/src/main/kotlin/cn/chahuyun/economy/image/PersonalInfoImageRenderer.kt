package cn.chahuyun.economy.image

import cn.chahuyun.economy.image.model.PersonalInfoCard
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO

/**
 * 个人信息图渲染门面。
 *
 * Core 负责组装 PersonalInfoCard；image 负责头像读取、底图选择和最终绘制。
 */
object PersonalInfoImageRenderer {

    @JvmStatic
    @Throws(IOException::class)
    fun render(card: PersonalInfoCard, avatarUrl: String): BufferedImage {
        val avatar = ImageIO.read(URL(avatarUrl)) ?: throw IOException("头像读取失败: $avatarUrl")
        return EconomyImageRenderer.renderPersonalInfo(
            card = card,
            avatar = avatar,
            background = ImageManager.getNextBottom(),
            font = ImageManager.getCustomFont()
        )
    }
}
