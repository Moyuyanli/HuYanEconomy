package cn.chahuyun.economy.entity.props;

import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.prop.PropBase;
import cn.hutool.core.date.DateUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.Date;

/**
 * 卡类道具 继承道具基本属性<p>
 * 特有属性:<p>
 * [status] 状态 t 为使用中  仅针对有长期时效性的道具卡 例 7天签到双倍卡<p>
 * [operation] 操作 t 为可以操作开启关闭状态 f 不可操作 !!!暂时不用注意此属性 默认f<p>
 * [enabledTime] 启用时间 有时间效益的道具卡的启用时间<p>
 * [aging] 时效 单位 d|M|y 天|月|年<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 9:27
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
public class PropsCard extends PropBase implements Serializable {

    /**
     * 双倍签到卡
     */
    public final static String SIGN_2 = "sign-2";

    /**
     * 三倍签到卡
     */
    public final static String SIGN_3 = "sign-3";

    /**
     * 补签卡
     */
    public final static String SIGN_IN = "sign-in";

    /**
     * 签到月卡
     */
    public final static String MONTHLY = "sign-monthly";

    /**
     * 医保卡
     */
    public final static String HEALTH = "health";


    /**
     * 道具卡状态
     */
    @Getter
    @Builder.Default
    private boolean status = false;
    /**
     * 允许操作
     */
    @Builder.Default
    private boolean operation = false;
    /**
     * 启用时间
     */
    private Date enabledTime;
    /**
     * 时效
     */
    private String aging;

    public PropsCard() {
    }


    @Override
    public String toString() {
        switch (this.getCode()) {
            case MONTHLY:
                return "道具名称:" + this.getName() +
                        "\n价格:" + this.getCost() + "金币" +
                        "\n状态:" + (status ? "使用中" : "未使用") +
                        "\n过期时间:" + DateUtil.format(this.getExpiredTime(), "yyyy-MM-dd") +
                        "\n描述:" + this.getDescription();
            default:
                return "卡名称:" + this.getName() +
                        "\n价格:" + this.getCost() + "金币" +
                        "\n状态:" + (status ? "使用中" : "未使用") +
                        "\n描述:" + this.getDescription();
        }
    }

    /**
     * 商店显示描述
     *
     * @return 商店显示结果
     */
    @Override
    public String toShopInfo() {
        return "道具名称: " + this.getName() +
                "\n道具描述: " + this.getDescription() +
                "\n道具价值: " + this.getCost() + "金币";
    }

    /**
     * 使用该道具
     */
    @Override
    public void use(UserInfo user) {
        this.status = true;
        this.enabledTime = new Date();
    }

}
