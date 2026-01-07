package cn.chahuyun.economy.prop

import cn.chahuyun.economy.model.props.UseEvent
import java.io.Serializable
import java.util.*

/**
 * 道具使用结果
 * 用于替代原有的异常控制流
 *
 * @property success 表示道具使用是否成功
 * @property message 包含使用结果的相关消息或错误信息
 * @property shouldRemove 表示道具使用后是否应该被移除，默认为false
 * @property shouldUpdate 表示道具使用后是否应该更新状态，默认为true
 */
data class UseResult(
    val success: Boolean,
    val message: String,
    val shouldRemove: Boolean = false,
    val shouldUpdate: Boolean = true,
) {
    companion object {
        /**
         * 创建一个成功的使用结果
         *
         * @param message 成功消息
         * @param shouldRemove 使用后是否应该移除道具，默认为false
         * @return UseResult实例，success为true
         */
        fun success(message: String, shouldRemove: Boolean = false) = UseResult(true, message, shouldRemove)

        /**
         * 创建一个失败的使用结果
         *
         * @param message 失败消息
         * @return UseResult实例，success为false，shouldUpdate为false
         */
        fun fail(message: String) = UseResult(false, message, shouldRemove = false, shouldUpdate = false)
    }
}


/**
 * 基础道具接口：定义所有道具共有的核心属性
 * 该接口定义了游戏中道具的基本属性和行为规范，所有道具类型都应实现此接口
 */
interface BaseProp : Serializable {
    /**
     * 道具类型标识符
     * 用于区分不同类型的道具，如武器、防具、消耗品等
     */
    val kind: String

    /**
     * 道具唯一编码
     * 用于在系统中唯一标识一个道具
     */
    val code: String

    /**
     * 道具名称
     * 可变属性，允许在运行时修改道具名称
     */
    var name: String

    /**
     * 道具描述信息
     * 可变属性，包含道具的详细说明和使用效果描述
     */
    var description: String

    /**
     * 道具价格
     * 可变属性，表示道具在商店中的购买价格
     */
    var cost: Int

    /**
     * 是否可购买标识
     * 可变属性，控制道具是否在商店中可被购买
     */
    var canBuy: Boolean

    /**
     * 获取道具的商店展示信息
     * 将道具的基本信息格式化为适合在商店界面显示的字符串格式
     *
     * @return 格式化的商店展示信息字符串
     */
    fun toShopInfo(): String

    /**
     * 克隆当前属性对象，创建并返回一个新的BaseProp实例
     *
     * @return BaseProp 返回当前对象的一个克隆副本
     */
    fun copyProp(): BaseProp
}


/**
 * 可堆叠能力接口
 * 定义了具有堆叠特性的对象所需实现的属性和行为
 */
interface Stackable {
    /**
     * 堆叠数量
     * 表示当前堆叠的数量值
     */
    var num: Int

    /**
     * 堆叠单位
     * 表示堆叠的单位标识符
     */
    var unit: String

    /**
     * 是否可堆叠标识
     * 返回true表示该对象支持堆叠功能
     */
    val isStack: Boolean get() = true
}


/**
 * 时效能力接口
 * 定义了具有过期能力的对象所需实现的属性和方法
 */
interface Expirable {
    /**
     * 获取当前时间的属性
     */
    var getTime: Date

    /**
     * 过期天数属性
     * 用于设置对象的有效天数
     */
    var expireDays: Int  // 恢复语义化命名

    /**
     * 过期时间属性
     * 记录对象的实际过期时间点，可为空
     */
    var expiredTime: Date?

    /**
     * 是否可过期属性
     * 标识该对象是否启用过期机制
     */
    var canItExpire: Boolean

    /**
     * 判断对象是否已过期
     * @return Boolean true表示已过期，false表示未过期
     */
    fun isExpired(): Boolean = canItExpire && expiredTime?.before(Date()) ?: false
}


/**
 * 可使用能力接口
 */
interface Usable {

    /**
     * 使用后是否消耗
     */
    var isConsumption: Boolean

    /**
     * 使用道具的具体逻辑
     * @return 使用结果，包含反馈消息及后续处理指令
     */
    suspend fun use(event: UseEvent): UseResult
}

/**
 * 通用抽象道具基类：实现了 BaseProp 的基础存储
 */
abstract class AbstractProp(
    override val kind: String,
    override val code: String,
    override var name: String,
) : BaseProp, Cloneable {
    override var description: String = ""
    override var cost: Int = 0
    override var canBuy: Boolean = false

    override fun toShopInfo(): String = "道具名称: $name\n道具描述: $description\n道具价值: $cost 金币"

    override fun toString(): String =
        "道具名称: $name\n道具数量: ${if (this is Stackable) "${this.num} ${this.unit}" else 1}\n道具描述: $description"

    /**
     * 实现接口要求的克隆方法
     */
    @Suppress("UNCHECKED_CAST")
    override fun copyProp(): BaseProp {
        return this.clone() as BaseProp
    }

    /**
     * 重写 Object 的 clone 方法，并提升权限为 public
     */
    public override fun clone(): Any {
        return try {
            super.clone() // 调用 JVM 原生内存复制
        } catch (e: CloneNotSupportedException) {
            // 理论上只要实现了 Cloneable 接口，这里就不会报错
            throw InternalError(e)
        }
    }
}

/**
 * 消耗品基类：可堆叠、可使用
 */
abstract class ConsumableProp(kind: String, code: String, name: String) :
    AbstractProp(kind, code, name), Stackable, Usable {
    override var isConsumption: Boolean = false
    
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
    override var isConsumption: Boolean = false

    var status: Boolean = false
    var enabledTime: Date? = null
}

