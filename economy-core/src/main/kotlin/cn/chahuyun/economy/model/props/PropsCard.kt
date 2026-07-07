package cn.chahuyun.economy.model.props

import cn.chahuyun.economy.prop.CardProp
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.economy.prop.UseResult
import cn.chahuyun.economy.prop.effect.PropEffectRegistry
import cn.chahuyun.economy.utils.format

/**
 * 道具卡。
 *
 * 该类只保留卡片状态，具体使用效果交给 PropEffectRegistry 中注册的处理器。
 */
class PropsCard(
    kind: String = "card",
    code: String = "",
    name: String = "",
) : CardProp(kind, code, name), Stackable {

    companion object {
        const val SIGN_2 = "sign-2"
        const val SIGN_3 = "sign-3"
        const val SIGN_IN = "sign-in"
        const val MONTHLY = "sign-monthly"
        const val HEALTH = "health"
        const val NAME_CHANGE = "rename"
    }

    override var num: Int = 1
    override var unit: String = "张"
    override var isStack: Boolean = true

    override suspend fun use(event: UseEvent): UseResult {
        return PropEffectRegistry.use(this, event) ?: UseResult.fail("该道具无法直接使用!")
    }

    override fun toShopInfo(): String {
        return when (code) {
            else -> "道具名称: $name\n道具描述: $description\n道具价值: $cost 金币"
        }
    }


    override fun toString(): String {
        // 1. 先根据类型确定差异化的文本行
        val extraInfo = when (code) {
            MONTHLY -> "过期时间: ${expiredTime?.format() ?: "永久"}"
            HEALTH -> "状态: ${if (status) "使用中" else "未使用"}"
            else -> return super.toString() // 如果不是这两种，直接返回
        }

        // 2. 使用单一的模板，确保所有行在代码中的缩进完全一致
        return """
        道具名称: $name
        道具数量: ${if (isStack) "${this.num} ${this.unit}" else 1}
        道具描述: $description
        $extraInfo
        道具价值: $cost 金币
        """.trimIndent()
    }
}
