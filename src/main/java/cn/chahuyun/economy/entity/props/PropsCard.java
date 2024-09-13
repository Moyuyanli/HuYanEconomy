package cn.chahuyun.economy.entity.props;

import cn.chahuyun.economy.props.PropsBase;
import lombok.Getter;
import lombok.Setter;

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
public class PropsCard extends PropsBase implements Serializable {

    /**
     * 道具卡状态
     */
    private boolean status;
    /**
     * 允许操作
     */
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

    /**
     * 创建一个道具模板
     * 具体实现方法请查看卡道具
     *
     * @param code        道具code
     * @param name        道具名称
     * @param cost        道具价值
     * @param stack       是否可以叠加物品
     * @param unit        道具数量单位
     * @param description 道具描述
     * @param reuse       是否复用
     * @param getTime     获取时间
     * @param expiredTime 过期时间
     * @param operation   可操作
     * @param aging       时效
     */
    public PropsCard(String code, String name, int cost, boolean stack, String unit, String description, boolean reuse, Date getTime, Date expiredTime, boolean operation, String aging) {
        super(code, name, cost, stack, unit, description, reuse, getTime, expiredTime);
        this.status = false;
        this.operation = operation;
        this.enabledTime = null;
        this.aging = aging;
    }

    @Override
    public String toString() {
        return "卡名称:" + this.getName() +
                "\n价格:" + this.getCost() + "金币" +
                "\n状态:" + (status ? "使用中" : "未使用") +
                "\n描述:" + this.getDescription();
    }

    /**
     * 使用该道具
     */
    @Override
    public void use() {

    }

    public boolean isStatus() {
        return status;
    }
}
