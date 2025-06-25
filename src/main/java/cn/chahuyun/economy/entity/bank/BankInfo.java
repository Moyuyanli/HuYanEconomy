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

    public static Integer randomInterest() {
        int i = RandomUtil.randomInt(1, 101);
        if (i <= 70) {
            return RandomUtil.randomInt(1, 3);
        } else if (i <= 99) {
            return RandomUtil.randomInt(3, 6);
        } else {
            return RandomUtil.randomInt(-3, 1);
        }
    }

}
