package cn.chahuyun.economy.entity.fish;

import cn.hutool.core.util.RandomUtil;
import jakarta.persistence.*;
import lombok.Data;

/**
 * 鱼
 *
 * @author Moyuyanli
 * @date 2022/12/9 9:50
 */
@Entity
@Table
@Data
public class Fish {
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
        int randomInt = RandomUtil.randomInt(dimensionsMin, dimensionsMax == dimensionsMin ? dimensionsMax + 1 : dimensionsMax);
        if (winning) {
            return (int) (randomInt + (randomInt * 0.2));
        } else {
            return randomInt;
        }
    }

}
