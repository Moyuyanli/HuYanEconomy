package cn.chahuyun.economy.prop

import java.util.*

/**
 * 道具注册中心
 */
object PropsType {

    private val props = LinkedHashMap<String, BaseProp>()
    private val map = HashMap<String, String>()

    @JvmStatic
    fun getPropsInfo(propCode: String): BaseProp? = props[propCode]

    @JvmStatic
    fun add(code: String, propBase: BaseProp) {
        props[code] = propBase
        map[getProps().size.toString()] = code
    }

    @JvmStatic
    fun getProps(): Map<String, BaseProp> = props

    @JvmStatic
    fun getNo(code: String): String? {
        for ((key, value) in map) {
            if (value == code) return key
        }
        return null
    }

    @JvmStatic
    fun getCode(no: String): String? {
        if (map.containsKey(no)) return map[no]
        for (value in props.values) {
            if (value.name == no) return value.kind
        }
        return null
    }
}
