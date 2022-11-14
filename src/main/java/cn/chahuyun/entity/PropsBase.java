package cn.chahuyun.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Date;

/**
 * 道具基本<p>
 * 所有道具都应该继承这个类 并重写 toString 方法<p>
 * 基本属性:<p>
 *  [id] 道具id 例 k-01 k-> 卡 编号01e<p>
 *  [name] 道具名称 例 补签卡<p>
 *  [description] 道具描述 例 此卡可以续上你断掉的某一天签到！<p>
 *  [reuse] 是否复用 例 false 补签卡不可复用<p>
 *  [getTime] 获取日期 默认 yyyy-MM-dd HH<p>
 *  [expiredTime] 到期日期 yyyy-MM-dd HH 到期默认 HH 为 4点<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 8:52
 */
@Entity
public class PropsBase {
    @Id
    private String id;
    /**
     * 道具id
     */
    private String code;
    /**
     * 道具名称
     */
    private String name;
    /**
     * 道具价值
     */
    private int cost;
    /**
     * 道具描述
     */
    private String description;
    /**
     * 是否可复用
     */
    private boolean reuse;
    /**
     * 获得时间
     */
    private Date getTime;
    /**
     * 过期时间
     */
    private Date expiredTime;

    public PropsBase() {
    }

    public PropsBase(String code, String name, int cost, String description, boolean reuse, Date getTime, Date expiredTime) {
        this.id = code;
        this.code = code;
        this.name = name;
        this.cost = cost;
        this.description = description;
        this.reuse = reuse;
        this.getTime = getTime;
        this.expiredTime = expiredTime;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.id = code;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isReuse() {
        return reuse;
    }

    public void setReuse(boolean reuse) {
        this.reuse = reuse;
    }

    public Date getGetTime() {
        return getTime;
    }

    public void setGetTime(Date getTime) {
        this.getTime = getTime;
    }

    public Date getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(Date expiredTime) {
        this.expiredTime = expiredTime;
    }

    public String toString() {
        return "请重写方法!";
    }
}


