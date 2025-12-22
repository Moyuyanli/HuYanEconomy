package cn.chahuyun.economy.prop

import cn.chahuyun.economy.model.props.UseEvent
import java.io.Serializable
import java.util.*

/**
 * 道具使用结果
 */
data class UseResult(
    val success: Boolean,
    val message: String,
    val shouldRemove: Boolean = false,
    val shouldUpdate: Boolean = true,
) {
    companion object {
        @JvmStatic
        fun success(message: String, shouldRemove: Boolean = false) = UseResult(true, message, shouldRemove)
        @JvmStatic
        fun fail(message: String) = UseResult(false, message, shouldRemove = false, shouldUpdate = false)
    }
}

/**
 * 核心道具接口：开闭原则的基础
 * 定义所有道具共有的核心属性
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
 * 功能接口：可堆叠
 */
interface Stackable {
    var num: Int
    var unit: String
    var stack: Boolean
    val isStack: Boolean get() = stack
}

/**
 * 功能接口：有时效性
 */
interface Expirable {
    var getTime: Date
    var expireDays: Int
    var expiredTime: Date?
    var canItExpire: Boolean

    /**
     * 是否已过期
     */
    fun isExpired(): Boolean = canItExpire && expiredTime?.before(Date()) ?: false
}

/**
 * 功能接口：可使用
 */
interface Usable {
    /**
     * 使用道具的具体逻辑
     * @return 使用结果
     */
    suspend fun use(event: UseEvent): UseResult
}

/**
 * 通用抽象道具基类：实现了核心属性的默认存储
 * 具体的道具类通过继承此类并实现相应功能接口来扩展功能
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

    override fun toString(): String {
        val countInfo = if (this is Stackable) "\n道具数量: ${this.num}" else ""
        return "道具名称: $name$countInfo\n道具描述: $description"
    }
}

/**
 * 消耗品模板：继承基础属性，并实现堆叠和使用功能
 */
abstract class ConsumableProp(kind: String, code: String, name: String) :
    AbstractProp(kind, code, name), Stackable, Usable {
    override var num: Int = 1
    override var unit: String = "个"
    override var stack: Boolean = true
    
    override suspend fun use(event: UseEvent): UseResult = UseResult.success("使用了 $name")
}

/**
 * 状态/卡片模板：继承基础属性，并实现时效和使用功能
 */
abstract class CardProp(kind: String, code: String, name: String) :
    AbstractProp(kind, code, name), Expirable, Usable {
    override var getTime: Date = Date()
    override var expireDays: Int = -1
    override var expiredTime: Date? = null
    override var canItExpire: Boolean = true

    var status: Boolean = false
    var enabledTime: Date? = null
    
    override suspend fun use(event: UseEvent): UseResult {
        status = true
        enabledTime = Date()
        return UseResult.success("已激活 $name")
    }
}
