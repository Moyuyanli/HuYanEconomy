package cn.chahuyun.economy.entity.fish;

import cn.hutool.core.util.RandomUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 鱼
 *
 * @author Moyuyanli
 * @date 2022/12/9 9:50
 */
@Entity(name = "Fish")
@Table
@Getter
@Setter
public class Fish implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * 等级
     */
    private int level;
    /**
     * 名称
     */
    private String name;
    /**
     * 描述
     */
    private String description;
    /**
     * 单价
     */
    private int price;
    /**
     * 最小尺寸
     */
    private int dimensionsMin;
    /**
     * 最大尺寸
     */
    private int dimensionsMax;
    private int dimensions1;
    private int dimensions2;
    private int dimensions3;
    private int dimensions4;
    /**
     * 难度
     */
    private int difficulty;
    /**
     * 特殊标记
     */
    private boolean special;

    /**
     * 获取鱼的尺寸<p>
     *
     * @param winning 当难度随机到200时，尺寸+20%
     * @return 鱼的尺寸
     */
    public int getDimensions(boolean winning) {
        int i = RandomUtil.randomInt(0, 101);
        int randomInt;
        if (i >= 90) {
            randomInt = RandomUtil.randomInt(dimensions3, dimensions4 == dimensions3 ? dimensions4 + 1 : dimensions4);
        } else if (i >= 70) {
            randomInt = RandomUtil.randomInt(dimensions2, dimensions3 == dimensions2 ? dimensions3 + 1 : dimensions3);
        } else {
            randomInt = RandomUtil.randomInt(dimensions1, dimensions2 == dimensions1 ? dimensions2 + 1 : dimensions2);
        }
        if (winning) {
            return (int) (randomInt + (randomInt * 0.2));
        } else {
            return randomInt;
        }
    }

    /**
     * 惊喜尺寸
     * @param surprise 是否惊喜
     * @param evolution 进化因子
     * @return 鱼的尺寸
     */
    public int getSurprise(boolean surprise, float evolution) {
        int dimensions = getDimensions(false);
        if (surprise) {
            return Math.round(dimensions * (1 + evolution));
        } else {
            return dimensions;
        }
    }

}
