package cn.chahuyun.economy.entity;

import cn.chahuyun.economy.util.HibernateUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 彩票信息
 *
 * @author Moyuyanli
 * @date 2022/12/6 8:55
 */
@Entity
@Table
@Getter
@Setter
public class LotteryInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * 购买用户
     */
    private long qq;
    /**
     * 购买群号
     */
    @Column(name = "group_number")
    private long group;
    /**
     * 购买金额
     */
    private double money;
    /**
     * 购买类型
     * 1:分钟彩票
     * 2:小时彩票
     * 3:天彩票
     */
    private int type;
    /**
     * 购买号码
     */
    private String number;
    /**
     * 本期号码
     */
    private String current;
    /**
     * 获得奖金
     */
    private double bonus;


    public LotteryInfo() {
    }

    /**
     * @param qq     购买用户
     * @param group  群号
     * @param money  金额
     * @param type   类型
     * @param number 购买号码
     * @author Moyuyanli
     * @date 2022/12/6 10:49
     */
    public LotteryInfo(long qq, long group, double money, int type, String number) {
        this.qq = qq;
        this.group = group;
        this.money = money;
        this.type = type;
        this.number = number;
    }

    /**
     * 保存
     */
    public void save() {
        HibernateUtil.factory.fromTransaction(session -> session.merge(this));
    }

    /**
     * 删除
     */
    public void remove() {
        HibernateUtil.factory.fromTransaction(session -> {
            session.remove(this);
            return null;
        });
    }

    /**
     * 转换为消息
     *
     * @author Moyuyanli
     * @date 2022/12/6 16:04
     */
    public String toMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        if (getType() == 1) {
            stringBuilder.append("你的小签已经开签了！");
        } else if (getType() == 2) {
            stringBuilder.append("你的中签已经开签了！");
        } else {
            stringBuilder.append("你的大签已经开签了！");
        }
        stringBuilder.append(String.format("\n本期号码:%s", getCurrent()))
                .append(String.format("\n你的号码:%s", getNumber()))
                .append(String.format("\n获得奖金为:%s", getBonus()));
        return stringBuilder.toString();
    }

}
