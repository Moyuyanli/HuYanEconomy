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

    /**
     * 注册道具类型到管理器
     *
     * @param kind 道具类型标识符
     * @param propClass 道具类的Class对象
     * @return 注册成功返回true，如果该类型已存在则返回false
     */
    @JvmStatic
    fun registerProp(kind: String, propClass: Class<out BaseProp>): Boolean {
        if (propsClassMap.containsKey(kind)) return false
        propsClassMap[kind] = propClass
        return true
    }

    /**
     * 检查指定的道具类型代码是否存在
     *
     * @param kind 道具类型标识符
     * @return 存在返回true，不存在返回false
     */
    @JvmStatic
    fun checkCodeExist(kind: String): Boolean = propsClassMap.containsKey(kind)

    /**
     * 根据用户背包信息获取道具实例
     *
     * @param backpack 用户背包对象
     * @return 成功返回道具实例，失败返回null
     */
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

    /**
     * 根据用户背包信息和指定类型获取道具实例
     *
     * @param backpack 用户背包对象
     * @param clazz 道具类型的Class对象
     * @return 返回指定类型的道具实例
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : BaseProp> getProp(backpack: UserBackpack, clazz: Class<T>): T {
        val propId = backpack.propId ?: error("错误,背包中道具id为空!")
        return deserialization(propId, clazz)
    }

    /**
     * 根据道具类型和ID获取道具实例
     *
     * @param kind 道具类型标识符
     * @param id 道具ID
     * @return 成功返回道具实例，失败返回null
     */
    @JvmStatic
    fun getProp(kind: String, id: Long): BaseProp? {
        val clazz = propsClassMap[kind] ?: return null
        return deserialization(id, clazz)
    }

    /**
     * 添加道具到数据库
     *
     * @param prop 道具实例
     * @return 返回新添加道具的ID
     */
    @JvmStatic
    fun addProp(prop: BaseProp): Long {
        val data = serialization(prop)
        return HibernateFactory.merge(data).id!!
    }

    /**
     * 更新数据库中的道具信息
     *
     * @param id 道具ID
     * @param prop 道具实例
     */
    @JvmStatic
    fun updateProp(id: Long, prop: BaseProp) {
        val data = serialization(prop)
        data.id = id
        HibernateFactory.merge(data)
    }

    /**
     * Java兼容的道具使用方法（同步调用）
     *
     * @param backpack 用户背包对象
     * @param event 使用事件对象
     * @return 使用结果
     */
    @JvmStatic
    fun usePropJava(backpack: UserBackpack, event: UseEvent) =
        runBlocking { return@runBlocking useProp(backpack, event) }

    /**
     * 异步使用道具
     *
     * @param backpack 用户背包对象
     * @param event 使用事件对象
     * @return 使用结果
     */
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

    /**
     * 根据ID销毁道具数据
     *
     * @param id 道具ID
     */
    @JvmStatic
    fun destroyPros(id: Long) {
        val data = HibernateFactory.selectOneById<PropsData>(id)
        if (data != null) HibernateFactory.delete(data)
    }

    /**
     * 销毁背包中的道具（同时删除道具数据和背包记录）
     *
     * @param propId 道具ID
     */
    @JvmStatic
    fun destroyProsInBackpack(propId: Long) {
        destroyPros(propId)
        val backpack = HibernateFactory.selectOne(UserBackpack::class.java, "propId", propId)
        if (backpack != null) HibernateFactory.delete(backpack)
    }

    /**
     * 核心字段同步序列化
     * 将道具对象序列化为数据库存储格式
     *
     * @param prop 道具实例
     * @return 序列化后的道具数据对象
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

    /**
     * 根据ID反序列化道具对象
     *
     * @param id 道具ID
     * @param clazz 道具类型的Class对象
     * @return 反序列化后的道具实例
     */
    @JvmStatic
    fun <T : BaseProp> deserialization(id: Long, clazz: Class<T>): T {
        val propsData = HibernateFactory.selectOneById<PropsData>(id)
            ?: throw RuntimeException("该道具数据不存在")

        val jsonConfig = JSONConfig.create().setIgnoreError(true)
        return JSONUtil.toBean(propsData.data, jsonConfig, clazz)
    }

    /**
     * 根据道具数据对象反序列化道具对象
     *
     * @param one 道具数据对象
     * @param clazz 道具类型的Class对象
     * @return 反序列化后的道具实例
     */
    @JvmStatic
    fun <T : BaseProp> deserialization(one: PropsData, clazz: Class<T>): T {
        val jsonConfig = JSONConfig.create().setIgnoreError(true)
        return JSONUtil.toBean(one.data, jsonConfig, clazz)
    }

    /**
     * 复制道具对象
     *
     * @param baseProp 原始道具实例
     * @return 复制后的道具实例
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : BaseProp> copyProp(baseProp: T): T {
        val data = serialization(baseProp)
        return deserialization(data, baseProp.javaClass)
    }

    /**
     * 获取指定类型标识符对应的道具类
     *
     * @param kind 道具类型标识符
     * @return 对应的Class对象，不存在返回null
     */
    @JvmStatic
    fun shopClass(kind: String): Class<out BaseProp>? = propsClassMap[kind]
}

