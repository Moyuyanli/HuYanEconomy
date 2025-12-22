package cn.chahuyun.economy.model.props

import cn.chahuyun.economy.manager.UserManager
import cn.chahuyun.economy.prop.*
import cn.chahuyun.hibernateplus.HibernateFactory
import java.util.*

/**
 * 道具卡 (Kotlin 重构版)
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
    override var stack: Boolean = true

    override suspend fun use(event: UseEvent): UseResult {
        return when (code) {
            NAME_CHANGE -> {
                val sender = event.sender
                val userInfo = UserManager.getUserInfo(sender)
                userInfo.name = sender.nick
                HibernateFactory.merge(userInfo)
                UseResult.success("改名卡使用成功!", shouldRemove = true)
            }

            else -> {
                super.use(event) // 使用 CardProp 的默认激活逻辑
            }
        }
    }

    override fun toShopInfo(): String {
        return "道具名称: $name\n道具描述: $description\n道具价值: $cost 金币"
    }
}
