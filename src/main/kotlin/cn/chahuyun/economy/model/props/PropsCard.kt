package cn.chahuyun.economy.model.props

import cn.chahuyun.economy.prop.*
import cn.chahuyun.economy.manager.UserManager
import cn.chahuyun.hibernateplus.HibernateFactory
import java.util.*

/**
 * 道具卡 (Kotlin 重构版)
 */
class PropsCard(
    kind: String = "card",
    code: String = "",
    name: String = ""
) : AbstractProp(kind, code, name), Expirable, Usable, Stackable {

    companion object {
        const val SIGN_2 = "sign-2"
        const val SIGN_3 = "sign-3"
        const val SIGN_IN = "sign-in"
        const val MONTHLY = "sign-monthly"
        const val HEALTH = "health"
        const val NAME_CHANGE = "rename"
    }

    override var getTime: Date = Date()
    override var expireDays: Int = -1
    override var expiredTime: Date? = null
    override var canItExpire: Boolean = true
    
    override var num: Int = 1
    override var unit: String = "张"
    
    @get:JvmName("isStatus")
    var status: Boolean = false
    var enabledTime: Date? = null

    override fun use(event: UseEvent): UseResult {
        return when (code) {
            NAME_CHANGE -> {
                val sender = event.sender ?: return UseResult.fail("发送者不存在")
                val userInfo = UserManager.getUserInfo(sender)
                userInfo.name = sender.nick
                HibernateFactory.merge(userInfo)
                UseResult.success("改名卡使用成功!", shouldRemove = true)
            }
            else -> {
                this.status = true
                this.enabledTime = Date()
                UseResult.success("道具卡已激活!")
            }
        }
    }

    override fun toShopInfo(): String {
        return "道具名称: $name\n道具描述: $description\n道具价值: $cost 金币"
    }
}

