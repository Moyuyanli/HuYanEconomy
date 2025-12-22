package cn.chahuyun.economy.model.title

import cn.chahuyun.economy.entity.TitleInfo
import cn.chahuyun.economy.entity.UserInfo

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

    override fun createTitleInfo(userInfo: UserInfo): TitleInfo {
        return TitleInfo(userInfo.qq, templateCode, titleName, title).apply {
            status = true
            this.impactName = this@TitleTemplateSimpleImpl.impactName ?: false
            this.gradient = this@TitleTemplateSimpleImpl.gradient ?: false
            this.sColor = this@TitleTemplateSimpleImpl.sColor
            this.eColor = this@TitleTemplateSimpleImpl.eColor
        }
    }
}
