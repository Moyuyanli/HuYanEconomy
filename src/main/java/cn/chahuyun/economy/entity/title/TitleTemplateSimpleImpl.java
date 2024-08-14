package cn.chahuyun.economy.entity.title;

import cn.chahuyun.economy.entity.TitleInfo;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.utils.ImageUtil;

import java.awt.*;

/**
 * 称号模板的简单实现
 *
 * @author Moyuyanli
 * @Date 2024/8/11 13:23
 */
public final class TitleTemplateSimpleImpl extends TitleTemplate {

    /**
     * 是否渐变
     */
    private final boolean gradient;

    /**
     * 是否影响名称
     */
    private final boolean impactName;

    /**
     * 称号
     */
    private final String title;

    /**
     * 起始颜色
     */
    private final String sColor;

    /**
     * 结束颜色
     */
    private final String eColor;

    /**
     * 创建一个模板，默认实现。<br>
     * 不可购买。
     *
     * @param templateCode   模版code
     * @param titleName      称号名称,不是称号
     * @param validityPeriod 过期天数
     * @param gradient       是否渐变
     * @param impactName     是否影响名称
     * @param title          称号名称
     * @param sColor         起始颜色
     * @param eColor         结束颜色
     */
    @Deprecated(since = "0.2.5")
    public TitleTemplateSimpleImpl(String templateCode, Integer validityPeriod, String titleName, boolean gradient, boolean impactName, String title, String sColor, String eColor) {
        super(templateCode, titleName, validityPeriod, null, 0.0);
        this.gradient = gradient;
        this.impactName = impactName;
        this.title = title;
        this.sColor = sColor;
        this.eColor = eColor;
    }

    /**
     * 创建一个模板，默认实现。<br>
     * 不可购买。
     *
     * @param templateCode   模版code
     * @param titleName      称号名称,不是称号
     * @param validityPeriod 过期天数
     * @param gradient       是否渐变
     * @param impactName     是否影响名称
     * @param title          称号名称
     * @param sColor         起始颜色
     * @param eColor         结束颜色
     */
    public TitleTemplateSimpleImpl(String templateCode, Integer validityPeriod, String titleName, boolean gradient, boolean impactName, String title, Color sColor, Color eColor) {
        super(templateCode, titleName, validityPeriod, null, 0.0);
        this.gradient = gradient;
        this.impactName = impactName;
        this.title = title;
        this.sColor = ImageUtil.colorHex(sColor);
        this.eColor = ImageUtil.colorHex(eColor);
    }


    /**
     * 创建一个可以购买的称号模板。
     *
     * @param templateCode   模版code
     * @param titleName      称号名称,不是称号
     * @param validityPeriod 过期天数
     * @param price          称号价格
     * @param gradient       是否渐变
     * @param impactName     是否影响名称
     * @param title          称号名称
     * @param sColor         起始颜色
     * @param eColor         结束颜色
     */
    @Deprecated(since = "0.2.5")
    public TitleTemplateSimpleImpl(String templateCode, Integer validityPeriod, String titleName, Double price, boolean gradient, boolean impactName, String title, String sColor, String eColor) {
        super(templateCode, titleName, validityPeriod, true, price);
        this.gradient = gradient;
        this.impactName = impactName;
        this.title = title;
        this.sColor = sColor;
        this.eColor = eColor;
    }

    /**
     * 创建一个可以购买的称号模板。
     *
     * @param templateCode   模版code
     * @param titleName      称号名称,不是称号
     * @param validityPeriod 过期天数
     * @param price          称号价格
     * @param gradient       是否渐变
     * @param impactName     是否影响名称
     * @param title          称号名称
     * @param sColor         起始颜色
     * @param eColor         结束颜色
     */
    public TitleTemplateSimpleImpl(String templateCode, Integer validityPeriod, String titleName, Double price, boolean gradient, boolean impactName, String title, Color sColor, Color eColor) {
        super(templateCode, titleName, validityPeriod, true, price);
        this.gradient = gradient;
        this.impactName = impactName;
        this.title = title;
        this.sColor = ImageUtil.colorHex(sColor);
        this.eColor = ImageUtil.colorHex(eColor);
    }

    /**
     * 创建称号的默认实现方法
     *
     * @param userInfo 用户信息
     * @return 称号
     */
    @Override
    public TitleInfo createTitleInfo(UserInfo userInfo) {
        return super.createTitleInfo(userInfo)
                .setGradient(this.gradient)
                .setImpactName(this.impactName)
                .setTitle(this.title)
                .setSColor(this.sColor)
                .setEColor(this.eColor);
    }
}
