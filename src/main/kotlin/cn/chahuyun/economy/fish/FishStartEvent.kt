package cn.chahuyun.economy.fish

import cn.chahuyun.economy.model.fish.FishBait
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.event.AbstractEvent

/**
 * 钓鱼开始事件
 *
 * @author Moyuyanli
 * @date 2024-11-14 10:15
 */
class FishStartEvent(
    val userInfo: UserInfoDto,
    val fishInfo: FishInfoDto
) : AbstractEvent() {
    /**
     * 鱼饵
     */
    var fishBait: FishBait? = null

    /**
     * 最大鱼等级
     */
    var maxGrade: Int? = null

    /**
     * 最大难度
     */
    var maxDifficulty: Int? = null

    /**
     * 最小难度
     */
    var minDifficulty: Int? = null

    /**
     * 计算最大等级
     *
     * @return 最大等级
     */
    fun calculateMaxGrade(): Int {
        return fishInfo.level
    }

    /**
     * 计算最大难度
     *
     * @return 最大难度
     */
    fun calculateMaxDifficulty(): Int {
        val baitLevel = fishBait?.level ?: 1
        return fishInfo.rodLevel * baitLevel
    }

    /**
     * 计算基本难度
     *
     * @return 基本难度
     */
    fun calculateMinDifficulty(): Int {
        val baitQuality = fishBait?.quality ?: 0.01f
        return Math.round(fishInfo.rodLevel * baitQuality)
    }
}

