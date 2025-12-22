package cn.chahuyun.economy.model.title

import cn.chahuyun.economy.entity.TitleInfo
import cn.chahuyun.economy.entity.UserInfo

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
    override fun createTitleInfo(userInfo: UserInfo): TitleInfo {
        return TitleInfo(userInfo.qq, templateCode, titleName, null)
    }
}
