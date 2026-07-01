package cn.chahuyun.economy.model.title

import cn.chahuyun.economy.model.user.TitleInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto

/**
 * 称号模板
 */
abstract class TitleTemplate(
    /**
     * 称号模板code
     */
    val templateCode: String,
    /**
     * 称号名称,不是称号
     */
    val titleName: String,
    /**
     * 过期天数
     */
    val validityPeriod: Int?,
    /**
     * 能否购买
     */
    val canIBuy: Boolean,
    /**
     * 价格
     */
    val price: Double?
) : TitleApi {
    override fun createTitleInfo(userInfo: UserInfoDto): TitleInfoDto {
        return TitleInfoDto(userId = userInfo.qq, code = templateCode, name = titleName)
    }
}
