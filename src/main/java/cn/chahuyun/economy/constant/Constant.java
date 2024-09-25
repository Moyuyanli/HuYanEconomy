package cn.chahuyun.economy.constant;

import cn.chahuyun.economy.entity.currency.GoldEconomyCurrency;
import xyz.cssxsh.mirai.economy.service.EconomyCurrency;

/**
 * 固定常量
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:43
 */
public class Constant {

    /**
     * 壶言日志
     */
    public static final String TOPIC = "HuYanEconomy";

    /**
     * 货币 [金币]
     */
    public static final EconomyCurrency CURRENCY_GOLD = new GoldEconomyCurrency();

}
