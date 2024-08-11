package cn.chahuyun.economy.entity.title;

import cn.chahuyun.economy.entity.TitleInfo;
import cn.chahuyun.economy.entity.UserInfo;
import lombok.Getter;

/**
 * @author Moyuyanli
 * @Date 2024/8/11 11:33
 */
@Getter
public abstract class TitleTemplate implements TitleApi {

    /**
     * 称号模板code
     */
    private final String templateCode;

    /**
     * 称号名称,不是称号
     */
    private final String titleName;

    /**
     * 过期天数
     */
    private final Integer validityPeriod;

    /**
     * 能否购买
     */
    private final Boolean canIBuy;

    /**
     * 价格
     */
    private final Double price;

    public TitleTemplate(String templateCode, String titleName, Integer validityPeriod, Boolean canIBuy, Double price) {
        this.templateCode = templateCode;
        this.titleName = titleName;
        this.validityPeriod = validityPeriod;
        this.canIBuy = canIBuy != null && canIBuy;
        this.price = price;
    }

    @Override
    public TitleInfo createTitleInfo(UserInfo userInfo) {
        return TitleInfo.builder()
                .userId(userInfo.getQq())
                .code(templateCode)
                .name(titleName)
                .build();
    }
}
