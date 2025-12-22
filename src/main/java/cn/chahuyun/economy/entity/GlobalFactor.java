package cn.chahuyun.economy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 全局因子
 *
 * @author Moyuyanli
 * @date 2024/9/26 9:20
 */
@Entity(name = "GlobalFactor")
@Table
@Getter
@Setter
public class GlobalFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    /**
     * 抢劫因子<br>
     * 基础抢劫成功概率
     */
    private Double robFactor = 0.4;


    /**
     * 抢劫银行因子<br>
     * 基础抢劫成功概率
     */
    private Double robBlankFactor = 0.01;


}
