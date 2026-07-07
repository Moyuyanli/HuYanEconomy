package cn.chahuyun.economy.model.fish

import kotlinx.serialization.Serializable

/**
 * 鱼种DTO
 */
@Serializable
data class FishDto(
    /** 记录ID */
    var id: Int = 0,
    /** 鱼的等级 */
    var level: Int = 0,
    /** 鱼的名称 */
    var name: String = "",
    /** 鱼的描述 */
    var description: String = "",
    /** 鱼的价格 */
    var price: Int = 0,
    /** 最小尺寸 */
    var dimensionsMin: Int = 0,
    /** 最大尺寸 */
    var dimensionsMax: Int = 0,
    /** 尺寸阈值1 */
    var dimensions1: Int = 0,
    /** 尺寸阈值2 */
    var dimensions2: Int = 0,
    /** 尺寸阈值3 */
    var dimensions3: Int = 0,
    /** 尺寸阈值4 */
    var dimensions4: Int = 0,
    /** 难度 */
    var difficulty: Int = 0,
    /** 是否为特殊鱼 */
    var special: Boolean = false
)
