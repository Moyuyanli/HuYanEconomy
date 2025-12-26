package cn.chahuyun.economy.prop

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUtil
import cn.hutool.json.JSONConfig
import cn.hutool.json.JSONUtil
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 道具管理器 (Kotlin 现代重构版)
 * 实现开闭原则，基于接口处理道具行为
 */
object PropsManager {

    private val propsClassMap = ConcurrentHashMap<String, Class<out BaseProp>>()

    @JvmStatic
    fun registerProp(kind: String, propClass: Class<out BaseProp>): Boolean {
        if (propsClassMap.containsKey(kind)) return false
        propsClassMap[kind] = propClass
        return true
    }

    @JvmStatic
    fun checkCodeExist(kind: String): Boolean = propsClassMap.containsKey(kind)

    @JvmStatic
    fun getProp(backpack: UserBackpack): BaseProp? {
        return try {
            val clazz = propsClassMap[backpack.propKind] ?: return null
            val propId = backpack.propId ?: error("错误,背包中道具id为空!")
            deserialization(propId, clazz)
        } catch (_: Exception) {
            HibernateFactory.delete(backpack)
            null
        }
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : BaseProp> getProp(backpack: UserBackpack, clazz: Class<T>): T {
        val propId = backpack.propId ?: error("错误,背包中道具id为空!")
        return deserialization(propId, clazz)
    }

    @JvmStatic
    fun getProp(kind: String, id: Long): BaseProp? {
        val clazz = propsClassMap[kind] ?: return null
        return deserialization(id, clazz)
    }

    @JvmStatic
    fun addProp(prop: BaseProp): Long {
        val data = serialization(prop)
        return HibernateFactory.merge(data).id!!
    }

    @JvmStatic
    fun updateProp(id: Long, prop: BaseProp) {
        val data = serialization(prop)
        data.id = id
        HibernateFactory.merge(data)
    }

    @JvmStatic
    fun usePropJava(backpack: UserBackpack, event: UseEvent) =
        runBlocking { return@runBlocking useProp(backpack, event) }

    suspend fun useProp(backpack: UserBackpack, event: UseEvent): UseResult {
        val prop = getProp(backpack) ?: return UseResult.fail("道具不存在")

        if (prop !is Usable) return UseResult.fail("该道具不可直接使用")

        val result = prop.use(event)

        if (result.success) {
            val propId = backpack.propId ?: error("错误,背包中道具id为空!")
            if (result.shouldRemove) {
                destroyProsInBackpack(propId)
            } else if (result.shouldUpdate) {
                if (prop is Stackable) {
                    if (prop.num > 1) {
                        prop.num--
                        updateProp(propId, prop)
                    } else {
                        destroyProsInBackpack(propId)
                    }
                } else {
                    updateProp(propId, prop)
                }
            }
        }

        return result
    }

    @JvmStatic
    fun destroyPros(id: Long) {
        val data = HibernateFactory.selectOneById<PropsData>(id)
        if (data != null) HibernateFactory.delete(data)
    }

    @JvmStatic
    fun destroyProsInBackpack(propId: Long) {
        destroyPros(propId)
        val backpack = HibernateFactory.selectOne(UserBackpack::class.java, "propId", propId)
        if (backpack != null) HibernateFactory.delete(backpack)
    }

    /**
     * 核心字段同步序列化
     */
    @JvmStatic
    fun serialization(prop: BaseProp): PropsData {
        // 核心字段提取
        val propsData = PropsData().apply {
            kind = prop.kind
            code = prop.code
        }

        // 处理时效性字段同步
        if (prop is Expirable && prop.canItExpire) {
            if (prop.expiredTime == null) {
                val days = if (prop.expireDays > 0) prop.expireDays else 1
                prop.expiredTime = DateUtil.offsetDay(Date(), days)
            }
            propsData.expiredTime = prop.expiredTime
        }

        // 处理堆叠字段同步
        if (prop is Stackable) {
            propsData.num = prop.num
        }

        // 处理状态字段同步 (针对卡片等)
        if (prop is cn.chahuyun.economy.model.props.PropsCard) {
            propsData.status = prop.status
        }

        propsData.data = JSONUtil.toJsonStr(prop)
        return propsData
    }

    @JvmStatic
    fun <T : BaseProp> deserialization(id: Long, clazz: Class<T>): T {
        val propsData = HibernateFactory.selectOneById<PropsData>( id)
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
