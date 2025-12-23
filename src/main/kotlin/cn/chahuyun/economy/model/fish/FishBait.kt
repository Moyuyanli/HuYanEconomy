package cn.chahuyun.economy.model.fish

import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.prop.ConsumableProp
import cn.chahuyun.economy.prop.UseResult

/**
 * 鱼饵 (Kotlin 重构版)
 */
class FishBait(
    kind: String = "fishBait",
    code: String = "",
    name: String = "",
) : ConsumableProp(kind, code, name) {

    companion object {
        const val BAIT_1 = "bait-1"
        const val BAIT_2 = "bait-2"
        const val BAIT_3 = "bait-3"
        const val BAIT_L_1 = "bait-l-1"
        const val BAIT_Q_1 = "bait-q-1"

        val fishbaitTimer = mapOf(
            BAIT_1 to 25,
            BAIT_2 to 20,
            BAIT_3 to 15,
            BAIT_L_1 to 18,
            BAIT_Q_1 to 18
        )
    }

    var level: Int = 1
    var quality: Float = 0.01f

    override suspend fun use(event: UseEvent): UseResult {
        if (num <= 0) return UseResult.fail("鱼饵已耗尽")
        // 逻辑保持简单，具体的消耗由 PropsManager 根据 num 自动处理
        return UseResult.success("使用了鱼饵")
    }

    override fun toShopInfo(): String {
        return "鱼饵名称: $name\n鱼饵等级: $level\n鱼饵品质: $quality\n剩余次数: $num\n价格: $cost 金币"
    }
}

