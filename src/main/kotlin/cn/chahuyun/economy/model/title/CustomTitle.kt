package cn.chahuyun.economy.model.title

import cn.chahuyun.economy.entity.TitleInfo
import cn.chahuyun.economy.entity.UserInfo

/**
 * 自定义称号模型
 */
data class CustomTitle(
    var templateCode: String = "",
    var validityPeriod: Int? = null,
    var titleName: String = "",
    var price: Double? = null,
    var gradient: Boolean? = null,
    var impactName: Boolean? = null,
    var title: String = "",
    var sColor: String = "",
    var eColor: String = ""
) {
    /**
     * 转换为称号模板
     *
     * @return 称号模板
     */
    fun toTemplate(): TitleTemplateSimpleImpl {
        return TitleTemplateSimpleImpl(
            templateCode,
            validityPeriod,
            titleName,
            price,
            gradient,
            impactName,
            title,
            sColor,
            eColor
        )
    }

    /**
     * 判断是否有空对象
     *
     * @return true 有
     */
    fun hasNullField(): Boolean {
        return templateCode.isBlank() || validityPeriod == null ||
                titleName.isBlank() || price == null || gradient == null ||
                impactName == null || title.isBlank() || sColor.isBlank() || eColor.isBlank()
    }
}
