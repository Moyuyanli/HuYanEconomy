package cn.chahuyun.economy.model.title;

import cn.chahuyun.economy.entity.TitleInfo;
import cn.chahuyun.economy.entity.UserInfo;
import lombok.Getter;

/**
 * 简单称号模板实现
 */
@Getter
public class TitleTemplateSimpleImpl extends TitleTemplate {

    private final Boolean gradient;
    private final Boolean impactName;
    private final String title;
    private final String sColor;
    private final String eColor;

    public TitleTemplateSimpleImpl(String templateCode, Integer validityPeriod, String titleName, Double price, Boolean gradient, Boolean impactName, String title, String sColor, String eColor) {
        super(templateCode, titleName, validityPeriod, true, price);
        this.gradient = gradient;
        this.impactName = impactName;
        this.title = title;
        this.sColor = sColor;
        this.eColor = eColor;
    }

    public TitleTemplateSimpleImpl(String templateCode, Integer validityPeriod, String titleName, Boolean canIBuy, Double price, Boolean gradient, Boolean impactName, String title, String sColor, String eColor) {
        super(templateCode, titleName, validityPeriod, canIBuy, price);
        this.gradient = gradient;
        this.impactName = impactName;
        this.title = title;
        this.sColor = sColor;
        this.eColor = eColor;
    }

    @Override
    public TitleInfo createTitleInfo(UserInfo userInfo) {
        return TitleInfo.builder()
                .userId(userInfo.getQq())
                .code(getTemplateCode())
                .name(getTitleName())
                .status(true)
                .impactName(impactName)
                .gradient(gradient)
                .title(title)
                .sColor(sColor)
                .eColor(eColor)
                .build();
    }
}

