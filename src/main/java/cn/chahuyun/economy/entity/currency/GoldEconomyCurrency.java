package cn.chahuyun.economy.entity.currency;

import cn.hutool.core.util.NumberUtil;
import org.jetbrains.annotations.NotNull;
import xyz.cssxsh.mirai.economy.service.EconomyCurrency;

/**
 * 货币 [金币]
 *
 * @author Moyuyanli
 * @date 2022/11/9 14:55
 */
public class GoldEconomyCurrency implements EconomyCurrency {
    @NotNull
    @Override
    public String getDescription() {
        return "闪闪发亮的金币";
    }

    @NotNull
    @Override
    public String getId() {
        return "hy-gold";
    }

    @NotNull
    @Override
    public String getName() {
        return "金币";
    }

    @NotNull
    @Override
    public String format(double amount) {
        return String.format("%s枚金币", NumberUtil.roundStr(amount, 0));
    }
}
