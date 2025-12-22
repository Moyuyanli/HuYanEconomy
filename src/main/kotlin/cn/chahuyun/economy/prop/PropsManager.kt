package cn.chahuyun.economy.prop

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUtil
import cn.hutool.json.JSONConfig
import cn.hutool.json.JSONUtil
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 道具管理器 (Kotlin 现代重构版)
 * 实现开闭原则，基于接口处理道具行为
 */
object PropsManager {

    private val propsClassMap = ConcurrentHashMap<String, Class<out BaseProp>>()

    /**
     * 注册道具类
     */
    @JvmStatic
    fun registerProp(kind: String, propClass: Class<out BaseProp>): Boolean {
        if (propsClassMap.containsKey(kind)) return false
        propsClassMap[kind] = propClass
        return true
    }

    /**
     * 检查指定类型的代码是否存在
     *
     * @param kind 代码类型标识符
     * @return 如果指定类型的代码存在则返回true，否则返回false
     */
    @JvmStatic
    fun checkCodeExist(kind: String): Boolean = propsClassMap.containsKey(kind)


    /**
     * 根据背包信息获取基础道具实例 (兼容 Java/Kotlin)
     */
    @JvmStatic
    fun getProp(backpack: UserBackpack): BaseProp? {
        return try {
            val clazz = propsClassMap[backpack.propKind] ?: return null
            deserialization(backpack.propId ?: return null, clazz)
        } catch (_: Exception) {
            HibernateFactory.delete(backpack)
            null
        }
    }

    /**
     * 根据背包信息获取指定类型的道具实例 (Java 调用版)
     * Java 调用: PropsManager.getProp(backpack, FishBait.class)
     */
    @JvmStatic
    fun <T : BaseProp> getProp(backpack: UserBackpack, clazz: Class<T>): T? {
        return try {
            val propId = backpack.propId ?: return null
            deserialization(propId, clazz)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 根据背包信息获取指定类型的道具实例 (Kotlin 调用版)
     * Kotlin 调用: PropsManager.getProp<FishBait>(backpack)
     */
    inline fun <reified T : BaseProp> getProp(backpack: UserBackpack): T? {
        return getProp(backpack, T::class.java)
    }

    /**
     * 根据类型和 ID 获取道具实例 (兼容 Java/Kotlin)
     */
    @JvmStatic
    fun getProp(kind: String, id: Long): BaseProp? {
        val clazz = propsClassMap[kind] ?: return null
        return try {
            deserialization(id, clazz)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 添加道具到持久化层
     */
    @JvmStatic
    fun addProp(prop: BaseProp): Long {
        val data = serialization(prop)
        return HibernateFactory.merge(data)!!.id!!
    }

    /**
     * 更新持久化层的道具数据
     */
    @JvmStatic
    fun updateProp(id: Long, prop: BaseProp) {
        val data = serialization(prop)
        data.id = id
        HibernateFactory.merge(data)
    }

    /**
     * 使用道具核心逻辑
     */
    @JvmStatic
    suspend fun useProp(backpack: UserBackpack, event: UseEvent): UseResult {
        val prop = getProp(backpack) ?: return UseResult.fail("道具不存在")

        if (prop !is Usable) return UseResult.fail("该道具不可直接使用")

        val result = prop.use(event)

        if (result.success) {
            if (result.shouldRemove) {
                destroyProsInBackpack(backpack.propId ?: return result)
            } else if (result.shouldUpdate) {
                if (prop is Stackable) {
                    if (prop.num > 1) {
                        prop.num--
                        updateProp(backpack.propId ?: return result, prop)
                    } else {
                        destroyProsInBackpack(backpack.propId ?: return result)
                    }
                } else {
                    updateProp(backpack.propId ?: return result, prop)
                }
            }
        }

        return result
    }

    @JvmStatic
    fun destroyPros(id: Long) {
        val data = HibernateFactory.selectOne(PropsData::class.java, id)
        if (data != null) HibernateFactory.delete(data)
    }

    @JvmStatic
    fun destroyProsInBackpack(propId: Long) {
        destroyPros(propId)
        val backpack = HibernateFactory.selectOne(UserBackpack::class.java, "propId", propId)
        if (backpack != null) HibernateFactory.delete(backpack)
    }

    /**
     * 序列化道具：将内存对象同步到数据库实体
     */
    @JvmStatic
    fun serialization(prop: BaseProp): PropsData {
        val propsData = PropsData().apply {
            kind = prop.kind
            code = prop.code
        }

        // 时效性处理
        if (prop is Expirable && prop.canItExpire) {
            if (prop.expiredTime == null) {
                val days = if (prop.expireDays > 0) prop.expireDays else 1
                prop.expiredTime = DateUtil.offsetDay(Date(), days)
            }
            propsData.expiredTime = prop.expiredTime
        }

        // 堆叠处理
        if (prop is Stackable) {
            propsData.num = prop.num
        }

        // 状态处理
        if (prop is CardProp) {
            propsData.status = prop.status
        }

        propsData.data = JSONUtil.toJsonStr(prop)
        return propsData
    }

    /**
     * 反序列化道具：从数据库数据恢复为内存对象
     */
    @JvmStatic
    fun <T : BaseProp> deserialization(id: Long, clazz: Class<T>): T {
        val propsData = HibernateFactory.selectOne(PropsData::class.java, id)
            ?: throw RuntimeException("该道具数据不存在")

        val jsonConfig = JSONConfig.create().setIgnoreError(true)
        return JSONUtil.toBean(propsData.data, jsonConfig, clazz)
    }

    @JvmStatic
    fun <T : BaseProp> deserialization(one: PropsData, clazz: Class<T>): T {
        val jsonConfig = JSONConfig.create().setIgnoreError(true)
        return JSONUtil.toBean(one.data, jsonConfig, clazz)
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : BaseProp> copyProp(baseProp: T): T {
        val data = serialization(baseProp)
        return deserialization(data, baseProp.javaClass)
    }

    @JvmStatic
    fun shopClass(kind: String): Class<out BaseProp>? = propsClassMap[kind]
}
