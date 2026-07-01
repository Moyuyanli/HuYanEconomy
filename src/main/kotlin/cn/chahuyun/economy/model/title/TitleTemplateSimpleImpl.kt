package cn.chahuyun.economy.model.title

import cn.chahuyun.economy.model.user.TitleInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto

/**
 * 简单称号模板实现
 */
class TitleTemplateSimpleImpl(
    templateCode: String,
    validityPeriod: Int?,
    titleName: String,
    canIBuy: Boolean = true,
    price: Double?,
    val gradient: Boolean? = null,
    val impactName: Boolean? = null,
    val title: String = "",
    val sColor: String = "",
    val eColor: String = ""
) : TitleTemplate(templateCode, titleName, validityPeriod, canIBuy, price) {

    constructor(
        templateCode: String,
        validityPeriod: Int?,
        titleName: String,
        price: Double?,
        gradient: Boolean?,
        impactName: Boolean?,
        title: String,
        sColor: String,
        eColor: String
    ) : this(
        templateCode = templateCode,
        validityPeriod = validityPeriod,
        titleName = titleName,
        canIBuy = true,
        price = price,
        gradient = gradient,
        impactName = impactName,
        title = title,
        sColor = sColor,
        eColor = eColor
    )

    override fun createTitleInfo(userInfo: UserInfoDto): TitleInfoDto {
        return TitleInfoDto(
            userId = userInfo.qq,
            code = templateCode,
            name = titleName,
            status = true,
            title = title,
            impactName = impactName ?: false,
            gradient = gradient ?: false,
            sColor = sColor,
            eColor = eColor
        )
    }
}
