package cn.chahuyun.economy.entity.fish;

import cn.chahuyun.economy.entity.props.UseEvent;
import cn.chahuyun.economy.prop.PropBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 鱼饵
 *
 * @author Moyuyanli
 * @date 2024-11-14 10:16
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class FishBait extends PropBase {

    /**
     * 基础鱼饵
     */
    public static final String BAIT_1 = "bait-1";
    /**
     * 中级鱼饵
     */
    public static final String BAIT_2 = "bait-2";
    /**
     * 高级鱼饵
     */
    public static final String BAIT_3 = "bait-3";
    /**
     * l形特化鱼饵一级
     */
    public static final String BAIT_L_1 = "bait-l-1";
    /**
     * q形特化鱼饵一级
     */
    public static final String BAIT_Q_1 = "bait-q-1";


    /**
     * 剩余使用次数
     */
    private Integer num;
    /**
     * 鱼饵等级
     */
    private Integer level;
    /**
     * 鱼饵品质
     */
    private Float quality;

    /**
     * 商店显示描述
     *
     * @return 商店显示结果
     */
    @Override
    public String toShopInfo() {
        return "鱼饵名称:" + this.getName() + "\n" +
                "鱼饵等级:" + this.getLevel() + "\n" +
                "鱼饵品质:" + this.getQuality() + "\n" +
                "鱼饵使用次数:" + this.getNum() + "\n" +
                "价格:" + this.getCost() + "金币" + "\n" +
                "描述:" + this.getDescription();
    }

    /**
     * 使用该道具
     *
     * @param info
     */
    @Override
    public void use(UseEvent info) {
        num--;
    }

    @Override
    public String toString() {
        return "鱼饵名称:" + this.getName() + "\n" +
                "鱼饵等级:" + this.getLevel() + "\n" +
                "鱼饵品质:" + this.getQuality() + "\n" +
                "剩余使用次数:" + this.getNum() + "\n" +
                "描述:" + this.getDescription();
    }
}
