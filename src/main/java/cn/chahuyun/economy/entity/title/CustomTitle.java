package cn.chahuyun.economy.entity.title;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class CustomTitle {
    private String templateCode;
    private Integer validityPeriod;
    private String titleName;
    private Double price;
    private Boolean gradient;
    private Boolean impactName;
    private String title;
    private String sColor;
    private String eColor;

    /**
     * 转换为称号模板
     *
     * @return 称号模板
     */
    public TitleTemplateSimpleImpl toTemplate() {
        return new TitleTemplateSimpleImpl(templateCode, validityPeriod, titleName, price, gradient, impactName, title, sColor, eColor);
    }

}
