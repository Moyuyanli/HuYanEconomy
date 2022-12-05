package cn.chahuyun.entity;

import cn.chahuyun.util.HibernateUtil;
import cn.chahuyun.util.Log;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户信息<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 9:45
 */
@Entity
@Table
@Getter
@Setter
public class UserInfo implements Serializable {


    @Id
    private Long id;
    /**
     * qq号
     */
    private long qq;
    /**
     * 名称
     */
    private String name;
    /**
     * 注册群号
     */
    private long registerGroup;
    /**
     * 注册时间
     */
    private Date registerTime;
    /**
     * 签到状态
     */
    private boolean sign;
    /**
     * 签到时间
     */
    private Date signTime;
    /**
     * 连续签到次数
     */
    private int signNumber = 0;
    /**
     * 断掉的连续签到次数
     */
    private int oldSignNumber;
    /**
     * 签到收益
     */
    private double signEarnings;
    /**
     * 银行收益
     */
    private double bankEarnings;

    /**
     * 道具背包
     */
    @OneToMany(targetEntity = UserBackpack.class, mappedBy = "userId", fetch = FetchType.EAGER)
    private List<UserBackpack> backpacks;

    public UserInfo() {
    }

    public UserInfo(long qq, long registerGroup, String name, Date registerTime) {
        this.id = qq;
        this.qq = qq;
        this.registerGroup = registerGroup;
        this.name = name;
        this.registerTime = registerTime;
    }

    public String getString() {
        return "用户名称:" + this.getName() +
                "\n用户qq:" + this.getQq() +
                "\n连续签到:" + this.getSignNumber() + "天\n";
    }

    /**
     * 签到
     *
     * @return boolean true 签到成功 false 签到失败
     * @author Moyuyanli
     * @date 2022/11/14 10:16
     */
    public boolean sign() {
//        if (true) return true;
        //如果签到时间为空->新用户第一次签到
        if (this.getSignTime() == null) {
            this.setSign(true);
            this.setSignTime(new Date());
            this.setSignNumber(1);
            HibernateUtil.factory.fromTransaction(session -> session.merge(this));
            return true;
        }
//        if (DateUtil.isSameDay(new Date(), this.getSignTime())) {
//            return false;
//        }
        //获取小时数差
        long between = DateUtil.between(new Date(), this.getSignTime(), DateUnit.HOUR, true);
        Log.debug("账户:(" + this.getQq() + ")签到天差->" + between);
        //时间还在24小时之内
        if (0 < between && between <= 24) {
            return false;
        } else if (24 < between && between <= 48) {
            this.setSignNumber(this.getSignNumber() + 1);
            this.setOldSignNumber(0);
        } else {
            this.setOldSignNumber(this.getSignNumber());
            this.setSignNumber(1);
        }
        this.setSign(true);
        this.setSignTime(new Date());
        HibernateUtil.factory.fromTransaction(session -> session.merge(this));
        return true;
    }

    /**
     * 将这个道具添加到用户背包<p>
     *
     * @param userBackpack 背包格信息
     * @return boolean  true 成功
     * @author Moyuyanli
     * @date 2022/11/28 15:55
     */
    public boolean addPropToBackpack(UserBackpack userBackpack) {
        this.getBackpacks().add(userBackpack);
        try {
            HibernateUtil.factory.fromTransaction(session -> session.merge(this));
            HibernateUtil.factory.fromTransaction(session -> session.merge(userBackpack));
        } catch (Exception e) {
            Log.error("用户信息:添加道具到背包出错", e);
            return false;
        }
        return true;
    }


    public void setQq(long qq) {
        this.id = qq;
        this.qq = qq;
    }


    public boolean isSign() {
        String now = DateUtil.format(new Date(), "yyyy-MM-dd") + " 04:00:00";
        DateTime nowDate = DateUtil.parse(now);
        long between = DateUtil.between(nowDate, signTime, DateUnit.HOUR, false);
        return between > 0;
    }

}
