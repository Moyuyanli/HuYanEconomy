package cn.chahuyun.economy.entity.fish;

import cn.chahuyun.economy.plugin.FishManager;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.HibernateUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 鱼塘
 *
 * @author Moyuyanli
 * @date 2022/12/8 14:37
 */
@Entity(name = "FishPond")
@Table
@Getter
@Setter
public class FishPond implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * 鱼塘code<p>
     * 群鱼塘 g[群号]<p>
     * 私人鱼塘 [群号]-[玩家qq]<p>
     * 私人全局鱼塘 [玩家qq]<p>
     */
    private String code;
    /**
     * 鱼塘管理者
     */
    private long admin;
    /**
     * 鱼塘类型<p>
     * 1-群鱼塘<p>
     * 2-私人鱼塘<p>
     * 3-全局鱼塘<p>
     */
    private int pondType;
    /**
     * 鱼塘名称
     */
    private String name;
    /**
     * 鱼塘描述
     */
    private String description;
    /**
     * 鱼塘等级
     */
    private int pondLevel;
    /**
     * 限制最低进入等级
     */
    private int minLevel;
    /**
     * 鱼塘钓的鱼出售回扣金额
     * 0.00-0.10
     */
    private double rebate;
    /**
     * 总钓鱼次数
     */
    private int number;

    @OneToMany(targetEntity = Fish.class, mappedBy = "id", fetch = FetchType.EAGER)
    private List<Fish> fishList;

    public FishPond() {
    }

    public FishPond(int pondType, long group, long admin, String name, String description) {
        if (pondType == 1) {
            this.code = "g-" + group;
        } else if (pondType == 2) {
            this.code = "g-" + group + "-" + admin;
        } else {
            this.code = String.valueOf(admin);
        }
        this.admin = admin;
        this.name = name;
        this.description = description;
        if (pondType == 1) {
            this.pondLevel = 6;
        } else {
            this.pondLevel = 1;
        }
        this.pondType = pondType;
        this.minLevel = 0;
        this.rebate = 0.05;
        this.number = 0;
    }

    /**
     * 获取鱼塘的经济
     *
     * @return double
     * @author Moyuyanli
     * @date 2022/12/12 9:56
     */
    public double getFishPondMoney() {
        return EconomyUtil.getMoneyByBankFromId(getCode(), getDescription());
    }

    /**
     * 获取池塘的鱼
     *
     * @return 池塘的鱼
     */
    public List<Fish> getFishList() {
        if (this.fishList == null) {
            return new ArrayList<>();
        }
        return fishList;
    }

    /**
     * 获取池塘的鱼
     *
     * @param level 鱼的等级
     * @return 池塘的鱼
     */
    public List<Fish> getFishList(int level) {
        if (pondType == 1) {
            return FishManager.getLevelFishList(level);
        }
        if (this.fishList == null) {
            return new ArrayList<>();
        }
        return fishList;
    }

    /**
     * 添加一次钓鱼次数
     */
    public void addNumber() {
        this.number++;
        save();
    }

    /**
     * 保存
     */
    public FishPond save() {
        return HibernateUtil.factory.fromTransaction(session -> session.merge(this));
    }

}
