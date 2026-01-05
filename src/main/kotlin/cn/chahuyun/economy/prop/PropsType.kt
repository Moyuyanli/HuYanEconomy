package cn.chahuyun.economy.prop

/**
 * 道具注册中心
 */
object PropsType {

    private val props = LinkedHashMap<String, BaseProp>()
    private val map = HashMap<String, String>()

    /**
     * 根据道具编码获取道具信息
     * @param propCode 道具编码
     * @return 道具对象，如果不存在则返回null
     */
    @JvmStatic
    fun getPropsInfo(propCode: String): BaseProp? = props[propCode]

    /**
     * 添加道具到注册中心
     * @param code 道具编码
     * @param propBase 道具基础对象
     */
    @JvmStatic
    fun add(code: String, propBase: BaseProp) {
        props[code] = propBase
        map[getProps().size.toString()] = code
    }

    /**
     * 获取所有注册的道具
     * @return 道具映射表
     */
    @JvmStatic
    fun getProps(): Map<String, BaseProp> = props

    /**
     * 根据道具编码获取序号
     * @param code 道具编码
     * @return 序号，如果不存在则返回null
     */
    @JvmStatic
    fun getNo(code: String): String? {
        for ((key, value) in map) {
            if (value == code) return key
        }
        return null
    }

    /**
     * 根据序号或道具名称获取道具编码
     * @param no 序号或道具名称
     * @return 道具编码，如果不存在则返回null
     */
    @JvmStatic
    fun getCode(no: String): String? {
        if (map.containsKey(no)) return map[no]
        for (value in props.values) {
            if (value.name == no) return value.kind
        }
        return null
    }
}
