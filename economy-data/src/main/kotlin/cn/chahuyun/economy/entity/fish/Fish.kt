package cn.chahuyun.economy.entity.fish

import jakarta.persistence.*
import java.io.Serializable

/**
 * 鱼
 *
 * @author Moyuyanli
 * @date 2022/12/9 9:50
 */
@Entity(name = "Fish")
@Table
class Fish(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    /**
     * 等级
     */
    var level: Int = 0,

    /**
     * 名称
     */
    var name: String? = null,

    /**
     * 描述
     */
    var description: String? = null,

    /**
     * 单价
     */
    var price: Int = 0,

    /**
     * 最小尺寸
     */
    var dimensionsMin: Int = 0,

    /**
     * 最大尺寸
     */
    var dimensionsMax: Int = 0,

    var dimensions1: Int = 0,
    var dimensions2: Int = 0,
    var dimensions3: Int = 0,
    var dimensions4: Int = 0,

    /**
     * 难度
     */
    var difficulty: Int = 0,

    /**
     * 特殊标记
     */
    var special: Boolean = false
) : Serializable
