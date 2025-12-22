package cn.chahuyun.economy.model.currency;

import cn.chahuyun.economy.constant.Constant;
import xyz.cssxsh.mirai.economy.service.EconomyCurrency;

/**
 * 金币货币模型
 */
public class GoldEconomyCurrency extends EconomyCurrency {

    public GoldEconomyCurrency() {
        super(Constant.CURRENCY_GOLD_CODE, Constant.CURRENCY_GOLD_NAME);
    }
}

