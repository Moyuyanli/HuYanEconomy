package cn.chahuyun.entity;

import cn.chahuyun.util.HibernateUtil;
import cn.chahuyun.util.Log;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.io.Serializable;
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
public class UserInfo  implements Serializable {

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
    /**
     * 断掉的连续签到次数
     */
    private int oldSignNumber;

    @OneToMany(targetEntity = UserBackpack.class, mappedBy = "userId")
    private final List<UserBackpack> backpacks = new ArrayList<>();


    public String getString() {
        return "用户名称:" + this.getName() +
                "\n用户qq:" + this.getQq() +
                "\n连续签到:" +this.getSignTime()+ "天\n";
    }

    /**
     * 签到
     *
     * @return boolean true 签到成功 false 签到失败
     * @author Moyuyanli
     * @date 2022/11/14 10:16
     */
    public boolean sign() {
        //如果签到时间为空->新用户第一次签到
        if (this.getSignTime() == null) {
            this.setSign(true);
            this.setSignTime(new Date());
            this.setSignNumber(1);
            HibernateUtil.factory.fromTransaction(session -> session.merge(this));
            return true;
        }
        //判断是否为同一天
        if (DateUtil.isSameDay(new Date(), this.getSignTime())) {
            return false;
        }
        //获取天数差
        long between = DateUtil.between(new Date(), this.getSignTime(), DateUnit.DAY, false);
        Log.debug("账户:(" + this.getQq() + ")签到天差->" + between);
        if (0 <= between && between <= 1) {
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

    public int getOldSignNumber() {
        return oldSignNumber;
    }

    public void setOldSignNumber(int oldSignNumber) {
        this.oldSignNumber = oldSignNumber;
    }

    public List<UserBackpack> getBackpacks() {
        return backpacks;
    }

}
