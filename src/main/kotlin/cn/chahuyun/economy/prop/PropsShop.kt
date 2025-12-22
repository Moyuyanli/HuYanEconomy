package cn.chahuyun.economy.prop

import cn.chahuyun.economy.utils.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * 道具商店
 */
object PropsShop {

    private val shop = ConcurrentHashMap<String, BaseProp>()

    @JvmStatic
    fun addShop(code: String, props: BaseProp) {
        if (shop.containsKey(code)) {
            Log.debug("道具已存在于商店中: $code")
            return
        }
        if (!PropsManager.checkCodeExist(props.kind)) {
            Log.error("道具类型未注册: ${props.kind}")
            return
        }
        shop[code] = props
    }

    @JvmStatic
    fun getShopInfo(): Map<String, String> {
        return shop.mapValues { it.value.toShopInfo() }
    }

    @JvmStatic
    fun getTemplate(code: String): BaseProp? = shop[code]

    @JvmStatic
    fun getTemplateByName(name: String): BaseProp? {
        return shop.values.find { it.name == name }
    }

    @JvmStatic
    fun <T : BaseProp> restore(code: String, tClass: Class<T>): T {
        val prop = shop[code] ?: throw RuntimeException("未找到对应code的道具: $code")
        if (!tClass.isInstance(prop)) {
            throw RuntimeException("道具类型不符: $code")
        }
        return tClass.cast(prop)
    }

    @JvmStatic
    fun checkPropExist(code: String): Boolean = shop.containsKey(code)

    @JvmStatic
    fun checkPropNameExist(name: String): Boolean = shop.values.any { it.name == name }
}
