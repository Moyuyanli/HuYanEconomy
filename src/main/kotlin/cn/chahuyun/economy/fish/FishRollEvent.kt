package cn.chahuyun.economy.fish

import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.fish.Fish
import cn.chahuyun.economy.entity.fish.FishInfo
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.model.fish.FishBait
import net.mamoe.mirai.event.AbstractEvent

/**
 * roll鱼阶段
 *
 * @author Moyuyanli
 * @date 2024-11-14 16:58
 */
class FishRollEvent(
    val userInfo: UserInfo,
    val fishInfo: FishInfo,
    val fishPond: FishPond,
    val fishBait: FishBait,
    val between: Float,
    val evolution: Float,
    var maxGrade: Int,
    var minGrade: Int,
    var maxDifficulty: Int,
    var minDifficulty: Int,
    var surprise: Boolean
) : AbstractEvent() {
    /**
     * 结果鱼
     */
    var fish: Fish? = null
}

