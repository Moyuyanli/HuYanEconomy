package cn.chahuyun.entity;

import cn.chahuyun.util.HibernateUtil;
import cn.chahuyun.util.Log;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import jakarta.persistence.*;

import java.util.ArrayList;
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
public class UserInfo {

    @Id
    private Long id;
    /**
     * qq号
     */
    private long qq;
    /**
     * 注册群号
     */
    private long registerGroup;
    /**
     * 名称
     */
    private String name;
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

    @OneToMany(targetEntity = UserBackpack.class ,mappedBy = "userId")
    private final List<UserBackpack> backpacks = new ArrayList<>();


    public String getString() {
        return "用户名称:" + this.getName() +
                "\n用户qq:" + this.getQq() +
                "\n是否签到:" + (isSign() ? "已签到" : "未签到") + "\n";
    }

    /**
     * 签到
     *
     * @return boolean true 签到成功 false 签到失败
     * @author Moyuyanli
     * @date 2022/11/14 10:16
     */
    public boolean sign() {
        if (this.getSignTime() == null) {
            this.setSign(true);
            this.setSignTime( new Date());
            HibernateUtil.factory.fromSession(session -> session.merge(this));
            return true;
        }
        String now = DateUtil.format(new Date(), "yyyy-MM-dd") + " 04:00:00";
        DateTime nowDate = DateUtil.parse(now);
        long between = DateUtil.between(nowDate, this.getSignTime(), DateUnit.HOUR, false);
        Log.debug("账户:(" + this.getQq() + ")签到时差->" + between);
//        System.out.println("between->"+between);
        if (between <= 0) {
            this.setSign(true);
            this.setSignTime( new Date());
            HibernateUtil.factory.fromSession(session -> session.merge(this));
            return true;
        }
        return false;
    }


    public UserInfo() {
    }

    public UserInfo(long qq, long registerGroup, String name, Date registerTime) {
        this.id = qq;
        this.qq = qq;
        this.registerGroup = registerGroup;
        this.name = name;
        this.registerTime = registerTime;
    }

    public Long getId() {
        return id;
    }

    public long getQq() {
        return qq;
    }

    public void setQq(long qq) {
        this.id = qq;
        this.qq = qq;
    }

    public long getRegisterGroup() {
        return registerGroup;
    }

    public void setRegisterGroup(long group) {
        this.registerGroup = group;
    }

    public Date getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Date registerTime) {
        this.registerTime = registerTime;
    }

    public Date getSignTime() {
        return signTime;
    }

    public void setSignTime(Date signTime) {
        this.signTime = signTime;
    }

    public int getSignNumber() {
        return signNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSignNumber(int signNumber) {
        this.signNumber = signNumber;
    }

    public boolean isSign() {
        String now = DateUtil.format(new Date(), "yyyy-MM-dd") + " 04:00:00";
        DateTime nowDate = DateUtil.parse(now);
        long between = DateUtil.between(nowDate, signTime, DateUnit.HOUR, false);
        return between > 0;
    }

    public void setSign(boolean sign) {
        this.sign = sign;
    }

    public List<UserBackpack> getBackpacks() {
        return backpacks;
    }

}
