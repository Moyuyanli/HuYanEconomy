package cn.chahuyun.economy.entity

import jakarta.persistence.*

/**
 * 全局因子
 *
 * @author Moyuyanli
 * @date 2024/9/26 9:20
 */
@Entity(name = "GlobalFactor")
@Table
class GlobalFactor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    /**
     * 抢劫因子
     * 基础抢劫成功概率
     */
    var robFactor: Double = 0.4,

    /**
     * 抢劫银行因子
     * 基础抢劫成功概率
     */
    var robBlankFactor: Double = 0.01
)
