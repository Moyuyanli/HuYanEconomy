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
        // 生成1到100之间的随机整数，用于决定利率值的范围
        int i = RandomUtil.randomInt(1, 101);
        // 当随机数大于等于99时，表示有较高的利率，生成3到5之间的随机整数作为利率值
        if (i >= 99) {
            return RandomUtil.randomInt(3, 6);
        } else if (i >= 35) {
            // 当随机数大于等于70时，表示有一般利率，生成1到3之间的随机整数作为利率值
            return RandomUtil.randomInt(1, 3);
        } else {
            // 当随机数小于70时，表示利率较低，生成-3到1之间的随机整数作为利率值
            return RandomUtil.randomInt(-3, 1);
        }
    }


}
