package cn.chahuyun.entity;

import jakarta.persistence.Entity;

import java.util.Date;

/**
 * 卡类道具 继承道具基本属性
 * 特有属性:
 * [status] 状态 t 为使用中  仅针对有长期时效性的道具卡 例 7天签到双倍卡
 * [operation] 操作 t 为可以操作开启关闭状态 f 不可操作 !!!暂时不用注意此属性 默认f
 * [enabledTime] 启用时间 有时间效益的道具卡的启用时间
 * [aging] 时效 单位 d|M|y 天|月|年
 *
 * @author Moyuyanli
 * @date 2022/11/14 9:27
 */
@Entity
public class PropCard extends PropsBase {

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

    @Override
    public String toString() {
        return  "卡名称:" + this.getName() +
                "价格:" + this.getCost() + "金币" +
                "状态:" + (status ? "使用中" : "未使用") +
                "描述:" + this.getDescription();
    }

    public PropCard() {
    }

    public PropCard(String code, String name, int cost, String description, boolean reuse, Date getTime, Date expiredTime, boolean status, boolean operation, Date enabledTime, String aging) {
        super(code, name, cost, description, reuse, getTime, expiredTime);
        this.status = status;
        this.operation = operation;
        this.enabledTime = enabledTime;
        this.aging = aging;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean isOperation() {
        return operation;
    }

    public void setOperation(boolean operation) {
        this.operation = operation;
    }

    public Date getEnabledTime() {
        return enabledTime;
    }

    public void setEnabledTime(Date enabledTime) {
        this.enabledTime = enabledTime;
    }

    public String getAging() {
        return aging;
    }

    public void setAging(String aging) {
        this.aging = aging;
    }
}
