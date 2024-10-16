package cn.chahuyun.economy.entity.props;

import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.prop.PropBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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
     * @param user
     */
    @Override
    public void use(UserInfo user) {
        if (electricity >= 5) {
            electricity -= 5;
        } else {
            throw new RuntimeException("电棒没电了!");
        }
    }
}
