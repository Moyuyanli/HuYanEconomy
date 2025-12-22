package cn.chahuyun.economy.entity.bank;

import cn.hutool.core.util.RandomUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 银行信息
 *
 * @author Moyuyanli
 * @date 2022/12/22 12:38
 */
@Entity(name = "BankInfo")
@Table(name = "BankInfo")
@Getter
@Setter
@Accessors(chain = true)
public class BankInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * 银行唯一id
     */
    private String code;
    /**
     * 银行名称
     */
    private String name;
    /**
     * 银行描述
     */
    private String description;
    /**
     * 银行管理者qq
     */
    private long qq;
    /**
     * 是否每周随机银行利率
     */
    private boolean interestSwitch;
    /**
     * 注册时间
     */
    private Date regTime;
    /**
     * 银行注册金额
     */
    private double regTotal;
    /**
     * 银行总金额
     */
    private double total;
    /**
     * 银行利率 i%
     */
    private int interest;

    public BankInfo() {
    }

    /**
     * 构造一个银行信息
     *
     * @param code        银行编码
     * @param name        银行名称
     * @param description 银行描述
     * @param qq          银行管理者
     * @param regTotal    注册金额
     */
    public BankInfo(String code, String name, String description, long qq, double regTotal) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.qq = qq;
        this.regTime = new Date();
        this.regTotal = regTotal;
        this.interestSwitch = true;
        this.interest = randomInterest();
    }

    /**
     * 随机生成一个利率值
     * <p>
     * 此方法用于模拟生成一个随机的利率值，用于表示用户对某个项目的利率程度
     * 利率值的生成遵循特定的概率分布，以模拟不同用户利率的多样性
     *
     * @return 随机生成的利率值，正数表示较高利率，负数或零表示较低利率
     */
    public static Integer randomInterest() {
        // 随机基数 [1, 100]
        int roll = RandomUtil.randomInt(1, 101);

        // 定义概率阈值（提高可读性和可维护性）
        final int HIGH_INTEREST_THRESHOLD = 99; // >=99 → 2% 概率
        final int MEDIUM_INTEREST_THRESHOLD = 35; // >=35 → 64% 概率，<35 → 34%

        if (roll >= HIGH_INTEREST_THRESHOLD) {
            return RandomUtil.randomInt(10, 21); // [10, 20]
        } else if (roll >= MEDIUM_INTEREST_THRESHOLD) {
            return RandomUtil.randomInt(1, 10);   // [1, 9]
        } else {
            return RandomUtil.randomInt(-10, 1); // [-10, 0]
        }
    }


}
