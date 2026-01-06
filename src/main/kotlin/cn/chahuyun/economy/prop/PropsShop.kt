package cn.chahuyun.economy.prop

import cn.chahuyun.economy.utils.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * 道具商店 (Kotlin 重构版)
 */
object PropsShop {

    private val shop = ConcurrentHashMap<String, BaseProp>()

    /**
     * 添加道具到商店
     */
    @JvmStatic
    internal fun addShop(code: String, props: BaseProp) {
        if (shop.containsKey(code)) {
            Log.debug("道具已存在于商店中: $code")
            return
        }
        if (!PropsManager.checkKindExist(props.kind)) {
            Log.error("道具类型未注册: ${props.kind}")
            return
        }
        shop[code] = props
    }

    /**
     * 获取商店中所有道具信息
     */
    @JvmStatic
    fun getShopInfo(): Map<String, String> {
        return shop.mapValues { it.value.toShopInfo() }
    }

    /**
     * 根据编码获取模板
     */
    @JvmStatic
    fun getTemplate(code: String): BaseProp? = shop[code]

    /**
     * 根据名称获取模板
     */
    @JvmStatic
    fun getTemplateByName(name: String): BaseProp {
        return shop.values.find { it.name == name } ?: throw RuntimeException("该道具不存在!")
    }

    /**
     * 恢复道具（反序列化前的校验）
     */
    @JvmStatic
    fun <T : BaseProp> restore(code: String, tClass: Class<T>): T {
        val prop = shop[code] ?: throw RuntimeException("未找到对应code的道具: $code")
        if (!tClass.isInstance(prop)) {
            throw RuntimeException("道具类型不符: $code")
        }
        return tClass.cast(prop)
    }

    /**
     * 检查 code 是否存在
     */
    @JvmStatic
    fun checkPropExist(code: String): Boolean = shop.containsKey(code)

    /**
     * 检查名称是否存在
     */
    @JvmStatic
    fun checkPropNameExist(name: String): Boolean = shop.values.any { it.name == name }
}

