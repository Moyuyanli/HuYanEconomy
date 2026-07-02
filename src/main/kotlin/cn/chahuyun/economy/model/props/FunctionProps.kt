package cn.chahuyun.economy.model.props

import cn.chahuyun.economy.constant.PropConstant.RED_EYES_CD
import cn.chahuyun.economy.prop.AbstractProp
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.economy.prop.Usable
import cn.chahuyun.economy.prop.UseResult
import cn.chahuyun.economy.prop.effect.PropEffectRegistry
import java.util.*

/**
 * 功能性道具。
 *
 * 该类只保留道具实例状态，具体使用效果交给 PropEffectRegistry 中注册的处理器。
 */
class FunctionProps(
    kind: String = "function",
    code: String = "",
    name: String = "",
) : AbstractProp(kind, code, name), Usable, Stackable {

    companion object {
        const val ELECTRIC_BATON = "baton"
        const val RED_EYES = "red-eyes"
        const val MUTE_1 = "mute-1"
        const val MUTE_30 = "mute-30"
        const val FARM_RAFFLE_BASIC = "farm-raffle-basic"
        const val FARM_RAFFLE_ADVANCED = "farm-raffle-advanced"
    }

    var enableTime: Date? = null
    var electricity: Int = 100
    var muteTime: Int = 0

    override var num: Int = 1
    override var unit: String = "个"
    override var isStack: Boolean = true

    override var isConsumption: Boolean = false

    override suspend fun use(event: UseEvent): UseResult {
        return PropEffectRegistry.use(this, event) ?: UseResult.fail("该道具无法直接使用!")
    }

    override fun toShopInfo(): String {
        return when (code) {
            RED_EYES -> "道具名称: $name\n价格: $cost 金币\n持续时间: $RED_EYES_CD 分钟\n描述: $description"
            ELECTRIC_BATON -> "道具名称: $name\n价格: $cost 金币\n电量: $electricity%\n描述: $description"
            else -> "道具名称: $name\n价格: $cost 金币\n描述: $description"
        }
    }

    override fun toString(): String {
        return when (code) {
            RED_EYES -> "道具名称: $name\n道具数量: ${if (isStack) "${this.num} ${this.unit}" else 1}\n持续时间: $RED_EYES_CD 分钟\n描述: $description"
            ELECTRIC_BATON -> "道具名称: $name\n道具数量: ${if (isStack) "${this.num} ${this.unit}" else 1}\n电量: $electricity%\n描述: $description"
            else -> super.toString()
        }
    }
}
