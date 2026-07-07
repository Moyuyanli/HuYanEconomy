package cn.chahuyun.economy.fish

import cn.chahuyun.economy.model.fish.FishBait
import cn.chahuyun.economy.model.fish.FishDto
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.fish.FishPondDto
import cn.chahuyun.economy.model.user.UserInfoDto
import net.mamoe.mirai.event.AbstractEvent

/**
 * roll鱼阶段
 *
 * @author Moyuyanli
 * @date 2024-11-14 16:58
 */
class FishRollEvent(
    val userInfo: UserInfoDto,
    val fishInfo: FishInfoDto,
    val fishPond: FishPondDto,
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
    var fish: FishDto? = null
}

