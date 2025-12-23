package cn.chahuyun.economy.model.props

import cn.chahuyun.economy.entity.UserInfo
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import java.util.*

/**
 * 道具使用事件
 */
class UseEvent(
    /**
     * 发送着
     */
    val sender: User?,
    /**
     * 发送群
     */
    val subject: Contact?,
    /**
     * 用户信息
     */
    val userInfo: UserInfo,
    /**
     * 时间
     */
    val time: Date = Date()
)
