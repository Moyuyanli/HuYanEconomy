package cn.chahuyun.economy.constant;

import cn.chahuyun.economy.entity.GoldEconomyCurrency;
import xyz.cssxsh.mirai.economy.service.EconomyCurrency;

/**
 * 固定常量
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:43
 */
public class Constant {

    /**
     * 货币 [金币]
     */
    public static final EconomyCurrency CURRENCY_GOLD = new GoldEconomyCurrency();
    /**
     * 签到双倍金币卡 k - 卡， QD- 签到，2 - 2倍，01 - 一次性,
     */
    public static final String SIGN_DOUBLE_SINGLE_CARD = "K-QD-2-01";

}
