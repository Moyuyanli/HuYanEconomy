package cn.chahuyun.economy.model.props

import cn.chahuyun.economy.prop.AbstractProp
import cn.chahuyun.economy.prop.Usable
import cn.chahuyun.economy.prop.UseResult

/**
 * 装备 (Kotlin 重构版)
 */
class Equipment(
    kind: String = "equipment",
    code: String = "",
    name: String = "",
) : AbstractProp(kind, code, name), Usable {

    override fun use(event: UseEvent): UseResult {
        // 装备逻辑：比如穿上装备，增加属性等
        return UseResult.success("已装备 $name")
    }

    override fun toShopInfo(): String {
        return "装备名称: $name\n装备描述: $description\n装备价值: $cost 金币"
    }
}

