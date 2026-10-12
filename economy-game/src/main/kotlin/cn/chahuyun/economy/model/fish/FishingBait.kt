package cn.chahuyun.economy.model.fish

import cn.chahuyun.economy.constant.FishingBaitConstant

/**
 * 钓鱼过程中使用的鱼饵运行时值。
 *
 * @property name 鱼饵名称
 * @property level 鱼饵等级（影响鱼等级下限）
 * @property quality 鱼饵品质（影响惊喜尺寸概率）
 */
data class FishingBait(val name: String, val level: Int, val quality: Float) {
    companion object {
        /** 未使用鱼饵时的“空钩”。 */
        val EMPTY: FishingBait = FishingBait("空钩", 1, 0.01f)

        fun of(constant: FishingBaitConstant?): FishingBait =
            constant?.let { FishingBait(it.displayName, it.level, it.quality) } ?: EMPTY
    }
}
