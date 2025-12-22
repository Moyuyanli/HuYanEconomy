package cn.chahuyun.economy.prop

import cn.chahuyun.economy.model.props.UseEvent
import java.io.Serializable
import java.util.*

/**
 * 道具使用结果
 * 用于替代原有的异常控制流
 */
data class UseResult(
    val success: Boolean,
    val message: String,
    val shouldRemove: Boolean = false,
    val shouldUpdate: Boolean = true,
) {
    companion object {
        fun success(message: String, shouldRemove: Boolean = false) = UseResult(true, message, shouldRemove)
        fun fail(message: String) = UseResult(false, message, shouldRemove = false, shouldUpdate = false)
    }
}

/**
 * 基础道具接口：定义所有道具共有的核心属性
 */
interface BaseProp : Serializable {
    val kind: String
    val code: String
    var name: String
    var description: String
    var cost: Int
    var canBuy: Boolean

    /**
     * 商店展示信息
     */
    fun toShopInfo(): String
}

/**
 * 可堆叠能力接口
 */
interface Stackable {
    var num: Int
    var unit: String
    val isStack: Boolean get() = true
}

/**
 * 时效能力接口
 */
interface Expirable {
    var getTime: Date
    var expireDays: Int  // 恢复语义化命名
    var expiredTime: Date?
    var canItExpire: Boolean

    /**
     * 是否已过期
     */
    fun isExpired(): Boolean = canItExpire && expiredTime?.before(Date()) ?: false
}

/**
 * 可使用能力接口
 */
interface Usable {
    /**
     * 使用道具的具体逻辑
     * @return 使用结果，包含反馈消息及后续处理指令
     */
    fun use(event: UseEvent): UseResult
}

/**
 * 通用抽象道具基类：实现了 BaseProp 的基础存储
 */
abstract class AbstractProp(
    override val kind: String,
    override val code: String,
    override var name: String,
) : BaseProp {
    override var description: String = ""
    override var cost: Int = 0
    override var canBuy: Boolean = false

    override fun toShopInfo(): String = "道具名称: $name\n道具描述: $description\n道具价值: $cost 金币"

    override fun toString(): String =
        "道具名称: $name\n道具数量: ${if (this is Stackable) this.num else 1}\n道具描述: $description"
}

/**
 * 消耗品基类：可堆叠、可使用
 */
abstract class ConsumableProp(kind: String, code: String, name: String) :
    AbstractProp(kind, code, name), Stackable, Usable {
    override var num: Int = 1
    override var unit: String = "个"
}

/**
 * 状态卡片基类：不可堆叠、有时效、可使用（激活）
 */
abstract class CardProp(kind: String, code: String, name: String) :
    AbstractProp(kind, code, name), Expirable, Usable {
    override var getTime: Date = Date()
    override var expireDays: Int = -1
    override var expiredTime: Date? = null
    override var canItExpire: Boolean = true

    var status: Boolean = false
    var enabledTime: Date? = null
}

