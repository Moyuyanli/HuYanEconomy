package cn.chahuyun.economy.entity.fish;

import cn.chahuyun.economy.entity.UserBackpack;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 鱼塘
 *
 * @author Moyuyanli
 * @date 2022/12/8 14:37
 */
@Entity
@Table
@Getter
@Setter
public class FishPond {

    /**
     * 鱼塘id<p>
     * 群鱼塘 g.[群号]<p>
     * 私人鱼塘 g.[群号].[玩家qq]<p>
     * 私人全局鱼塘 [玩家qq]<p>
     */
    @Id
    private String id;
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

    @OneToMany(targetEntity = Fish.class, mappedBy = "id", fetch = FetchType.EAGER)
    private List<Fish> fishList;

    public FishPond() {
    }

    public FishPond(int pondType, long group, long admin, String name, String description) {
        if (pondType == 1) {
            this.id = "g." + group;
        } else if (pondType == 2) {
            this.id = "g." + group + "." + admin;
        } else {
            this.id = admin + ".";
        }
        this.admin = admin;
        this.name = name;
        this.description = description;
        if (pondType == 1) {
            this.pondLevel = 6;
        } else {
            this.pondLevel = 1;
        }
        this.minLevel = 0;
        this.rebate = 0.05;
    }

    public List<Fish> getFishList() {
        if (this.fishList==null) {
            return new ArrayList<>();
        }
        return fishList;
    }
}
