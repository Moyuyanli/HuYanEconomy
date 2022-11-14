package cn.chahuyun.entity;

import cn.chahuyun.util.HibernateUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import jakarta.persistence.*;

import java.util.Date;

import static cn.chahuyun.HuYanEconomy.log;

/**
 * 用户信息
 *
 *
 * @author Moyuyanli
 * @date 2022/11/14 9:45
 */
@Entity
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * qq号
     */
    private long qq;

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
    private int signNumber;

    @Override
    public String toString() {
        return "UserInfo{" +
                "qq=" + qq +
                ", registerTime=" + registerTime +
                ", signTime=" + signTime +
                ", signNumber=" + signNumber +
                '}';
    }

    /**
     * 签到
     *
     * @return boolean true 签到成功 false 签到失败
     * @author Moyuyanli
     * @date 2022/11/14 10:16
     */
    public boolean sign() {
        String now = DateUtil.format(new Date(), "yyyy-MM-dd")+" 04:00:00";
        DateTime nowDate = DateUtil.parse(now);
        long between = DateUtil.between(nowDate, signTime, DateUnit.HOUR, false);
        log.debug("账户:("+this.getQq()+")签到时差->"+between);
//        System.out.println("between->"+between);
        if (between <= 0) {
            sign = true;
            signTime = new Date();
            HibernateUtil.factory.fromSession(session -> session.merge(this));
            return true;
        }
        return false;
    }


    public UserInfo() {
    }

    public UserInfo(long qq, Date registerTime) {
        this.qq = qq;
        this.registerTime = registerTime;
        this.signTime = registerTime;
        this.signNumber = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getQq() {
        return qq;
    }

    public void setQq(long qq) {
        this.qq = qq;
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

    public void setSignNumber(int signNumber) {
        this.signNumber = signNumber;
    }

    public boolean isSign() {
        String now = DateUtil.format(new Date(), "yyyy-MM-dd")+" 04:00:00";
        DateTime nowDate = DateUtil.parse(now);
        long between = DateUtil.between(nowDate, signTime, DateUnit.HOUR, false);
        return between > 0;
    }

    public void setSign(boolean sign) {
        this.sign = sign;
    }
}
