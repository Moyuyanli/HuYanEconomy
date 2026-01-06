package cn.chahuyun.economy.entity

import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.PropsManager
import jakarta.persistence.*
import java.io.Serializable

/**
 * 用户背包
 *
 * @author Moyuyanli
 * @date 2022/11/15 9:02
 */
@Entity(name = "UserBackpack")
@Table
class UserBackpack(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long? = null,

    /**
     * 用户id
     */
    val userId: String? = null,

    /**
     * 道具编码
     */
    val propCode: String? = null,

    /**
     * 道具类型
     */
    val propKind: String? = null,

    /**
     * 道具id
     */
    val propId: Long? = null
) : Serializable {

    /**
     * 获取该背包道具
     *
     * @return 道具
     */
    inline fun <reified T : BaseProp> getProp(): T {
        return PropsManager.getProp(this, T::class.java)
    }

    /**
     * 获取指定类型的属性对象
     *
     * @param T 泛型类型参数，必须继承自BaseProp
     * @param tClass 要获取的属性类型的Class对象
     * @return 返回指定类型的属性实例
     */
    fun <T : BaseProp> getProp(tClass: Class<T>): T {
        return PropsManager.getProp(this, tClass)
    }

    /**
     * 获取该背包道具 (兼容旧版 API)
     */
    @Deprecated("使用泛型版本 getProp(clazz)", ReplaceWith("getProp()"))
    fun getPropLegacy(): BaseProp? {
        return PropsManager.getProp(this)
    }
}
