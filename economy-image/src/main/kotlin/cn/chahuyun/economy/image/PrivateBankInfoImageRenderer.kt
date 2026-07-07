package cn.chahuyun.economy.image

import cn.chahuyun.economy.image.model.PrivateBankInfoCard
import java.awt.image.BufferedImage

/**
 * 银行信息图渲染门面。
 *
 * Core 负责组装 PrivateBankInfoCard；image 负责选择字体和最终绘制。
 */
object PrivateBankInfoImageRenderer {

    @JvmStatic
    fun render(card: PrivateBankInfoCard): BufferedImage {
        return EconomyImageRenderer.renderPrivateBankInfo(
            card = card,
            font = ImageManager.getCustomFont()
        )
    }
}
