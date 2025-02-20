package cn.chahuyun.economy.entity.props;

import cn.chahuyun.economy.entity.UserFactor;
import cn.chahuyun.economy.exception.Operation;
import cn.chahuyun.economy.plugin.FactorManager;
import cn.chahuyun.economy.prop.PropBase;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Date;

/**
 * 功能性道具
 *
 * @author Moyuyanli
 * @date 2024-10-15 11:36
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class FunctionProps extends PropBase {

    /**
     * 便携电棍
     */
    public final static String ELECTRIC_BATON = "baton";

    /**
     * 红牛
     */
    public final static String RED_EYES = "red-eyes";

    /**
     * 1分钟禁言卡
     */
    public final static String MUTE_1 = "mute-1";

    /**
     * 生效时间
     */
    private Date enableTime;

    /**
     * 电量(剩余使用次数)
     */
    private Integer electricity;

    /**
     * 商店显示描述
     *
     * @return 商店显示结果
     */
    @Override
    public String toShopInfo() {
        return "道具名称:" + this.getName() +
                "\n价格:" + this.getCost() + "金币" +
                "\n描述:" + this.getDescription();
    }

    @Override
    public String toString() {
        switch (super.getCode()) {
            case ELECTRIC_BATON:
                return "道具名称:" + this.getName() +
                        "\n价格:" + this.getCost() + "金币" +
                        "\n剩余电量:" + this.getElectricity() + "%" +
                        "\n描述:" + this.getDescription();
            default:
                return "道具名称:" + this.getName() +
                        "\n价格:" + this.getCost() + "金币" +
                        "\n描述:" + this.getDescription();
        }
    }

    /**
     * 使用该道具
     *
     * @param info 使用信息
     */
    @Override
    public void use(UseEvent info) {
        switch (this.getCode()) {
            case RED_EYES:
                UserFactor factor = FactorManager.getUserFactor(info.getUserInfo());
                String buff = factor.getBuffValue(RED_EYES);
                if (buff == null) {
                    factor.setBuffValue(RED_EYES, DateUtil.now());
                    FactorManager.merge(factor);
                    throw new Operation("你猛猛炫了一瓶红牛!", true);
                } else {
                    DateTime parse = DateUtil.parse(buff);
                    long between = DateUtil.between(new Date(), parse, DateUnit.MINUTE);
                    if (between > 10) {
                        factor.setBuffValue(RED_EYES, DateUtil.now());
                        FactorManager.merge(factor);
                        throw new Operation("续上一瓶红牛!", true);
                    } else {
                        throw new Operation("红牛喝多了可对肾不好!");
                    }
                }
            case ELECTRIC_BATON:
                if (electricity >= 5) {
                    electricity -= 5;
                } else {
                    throw new RuntimeException("电棒没电了!");
                }
                break;
            default:
                throw new Operation("该道具无法直接使用!");
        }
    }
}
