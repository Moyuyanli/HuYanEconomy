@file:Suppress("unused")

package cn.chahuyun.economy.prop

import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUtil
import cn.hutool.json.JSONConfig
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 道具管理器 (Kotlin 现代重构版)
 * 实现开闭原则，基于接口处理道具行为
 */
object PropsManager {

    private fun isInvalidCode(code: String?): Boolean {
        return code.isNullOrBlank() || code == "code"
    }

    private fun applyCodeIfPossible(prop: BaseProp, code: String?) {
        if (isInvalidCode(code)) return
        if (prop is AbstractProp && isInvalidCode(prop.code)) {
            prop.code = code!!
        }
    }

    private val propsClassMap = ConcurrentHashMap<String, Class<out BaseProp>>()

    @PublishedApi
    internal val propsTemplateMap = ConcurrentHashMap<String, BaseProp>()

    /**
     * 注册道具类型到管理器
     *
     * @param kind 道具类型标识符
     * @param propClass 道具类的Class对象
     * @return 注册成功返回true，如果该类型已存在则返回false
     */
    @JvmStatic
    fun registerKindToPropClass(kind: String, propClass: Class<out BaseProp>): Boolean {
        if (propsClassMap.containsKey(kind)) return false
        propsClassMap[kind] = propClass
        return true
    }


    /**
     * 注册代码到道具映射表中
     *
     * 如果该道具可以购买,同步注册到商店
     *
     * @param prop 要注册的基础道具对象
     * @param code 要注册的道具代码，如果为null则使用prop.code作为默认值
     * @return 注册成功返回true，如果该代码已存在则返回false
     */
    @JvmStatic
    fun registerCodeToProp(prop: BaseProp, code: String? = null): Boolean {
        if (!propsClassMap.containsKey(prop.kind)) {
            Log.debug("道具模板的类型未注册!")
            return false
        }
        val onlyCode = code ?: prop.code
        // 检查道具代码是否已存在，存在则返回false
        if (propsTemplateMap.containsKey(onlyCode)) return false
        // 将道具添加到模板映射表中
        propsTemplateMap[onlyCode] = prop
        // 如果道具可购买，则添加到商店中
        if (prop.canBuy) PropsShop.addShop(onlyCode, prop)
        return true
    }


    /**
     * 检查指定的道具类型代码是否存在
     *
     * @param kind 道具类型标识符
     * @return 存在返回true，不存在返回false
     */
    @JvmStatic
    fun checkKindExist(kind: String): Boolean = propsClassMap.containsKey(kind)

    /**
     * 检查指定的道具code是否存在
     *
     * @param code 道具code标识符
     * @return 存在返回true，不存在返回false
     */
    @JvmStatic
    fun checkCodeExist(code: String): Boolean = propsTemplateMap.containsKey(code)

    /**
     * 【Kotlin 专用】获取道具模板副本
     * 利用 reified 关键字实化泛型，支持运行时类型检查
     * * 调用示例：val weapon = PropManager.getTemplate<WeaponProp>("W001")
     */
    inline fun <reified T : BaseProp> getTemplate(code: String): T {
        val prop = propsTemplateMap[code] ?: error("获取道具模板失败, 道具 code: [$code] 不存在!")
        return prop.copyProp()
    }

    /**
     * 【Java 专用】获取道具模板副本
     * 通过 Class 显式传递类型信息
     * * 调用示例：WeaponProp w = PropManager.getTemplate("W001", WeaponProp.class)
     */
    @JvmStatic
    @JvmName("getTemplate") // 确保 Java 端看到的名称是 getTemplate
    fun <T : BaseProp> getTemplate(code: String, clazz: Class<T>): T {
        val prop =
            propsTemplateMap[code] ?: throw IllegalArgumentException("获取道具模板失败, 道具 code: [$code] 不存在!")
        return prop.copyProp()
    }


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
        } catch (e: Exception) {
            Log.error("获取背包道具失败(ID: ${backpack.id}), 可能数据已损坏: ${e.message}")
            null
        }
    }

    /**
     * 根据用户背包信息和指定类型获取道具实例
     *
     * @param backpack 用户背包对象
     * @return 返回指定类型的道具实例
     */
    @JvmName("getPropWithType")
    inline fun <reified T : BaseProp> getProp(backpack: UserBackpack): T {
        return getProp(backpack, T::class.java)
    }

    /**
     * 根据用户背包信息和指定类型获取道具实例
     *
     * java 专用
     *
     * @param backpack 用户背包对象
     * @param clazz 道具类型的Class对象
     * @return 返回指定类型的道具实例
     */
    @JvmStatic
    fun <T : BaseProp> getProp(backpack: UserBackpack, clazz: Class<T>): T {
        // 检查背包中道具id是否为空，为空则抛出异常
        val propId = backpack.propId ?: throw IllegalArgumentException("背包中道具id为空!")
        // 通过道具id和类型进行反序列化获取道具实例
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
        // 防止历史数据/反序列化缺陷导致 code 被写空。
        // 若当前对象 code 无效，则尝试从数据库已存在的 PropsData 回填。
        if (isInvalidCode(prop.code)) {
            val existing = HibernateFactory.selectOneById<PropsData>(id)
            val fallback = existing?.code
                ?.takeIf { !isInvalidCode(it) }
                ?: existing?.data
                    ?.let {
                        runCatching {
                            JSONUtil.parseObj(it).getStr("code")
                        }.getOrNull()
                    }
                    ?.takeIf { !isInvalidCode(it) }

            applyCodeIfPossible(prop, fallback)
        }

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
                destroyProsAndBackpack(propId)
            } else if (result.shouldUpdate) {
                if (prop is Stackable && prop.isConsumption) {
                    if (prop.num > 1) {
                        prop.num--
                        updateProp(propId, prop)
                    } else {
                        destroyProsAndBackpack(propId)
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
    fun destroyProsAndBackpack(propId: Long) {
        destroyPros(propId)
        val backpack = HibernateFactory.selectOne(UserBackpack::class.java, "propId", propId)
        if (backpack != null) HibernateFactory.delete(backpack)
    }

    /**
     * 销毁背包中的道具（同时删除道具数据和背包记录）
     *
     * @param backpack 用户背包对象，包含要销毁的道具信息
     */
    @JvmStatic
    fun destroyProsAndBackpackByBackpack(backpack: UserBackpack) {
        val propId = backpack.propId ?: error("销毁道具错误,背包无对应道具id")
        // 销毁道具数据
        destroyPros(propId)
        // 查询并删除背包记录
        val userBackpack = HibernateFactory.selectOne(UserBackpack::class.java, "propId", propId)
        if (userBackpack != null) HibernateFactory.delete(userBackpack)
    }

    /**
     * 扩展函数，销毁当前背包中的道具
     * 调用destroyProsAndBackpackByBackpack方法销毁当前背包对象对应的道具
     */
    fun UserBackpack.destroy() = destroyProsAndBackpackByBackpack(this)


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
            ?: throw RuntimeException("该道具数据不存在, ID: $id")

        val jsonConfig = JSONConfig.create().setIgnoreError(true)
        val data = propsData.data ?: throw RuntimeException("道具数据内容为空，ID: $id")

        return try {
            val jsonObject = JSONUtil.parseObj(data)

            // 优先以“列->JSON”的方式补齐 code，避免 Kotlin val/父类字段导致 toBean 丢失 code。
            if (isInvalidCode(jsonObject.getStr("code")) && !isInvalidCode(propsData.code)) {
                jsonObject["code"] = propsData.code
            }
            if (isInvalidCode(jsonObject.getStr("kind")) && !propsData.kind.isNullOrBlank()) {
                jsonObject["kind"] = propsData.kind
            }

            val prop = JSONUtil.toBean(jsonObject.toString(), jsonConfig, clazz)
                ?: throw RuntimeException("道具反序列化结果为空")

            // 最终兜底：如果 toBean 仍然未能正确注入 code，则从 JSON/列回填到对象。
            applyCodeIfPossible(prop, jsonObject.getStr("code") ?: propsData.code)
            prop
        } catch (e: Exception) {
            throw RuntimeException("道具反序列化失败，ID: $id, 目标类型: ${clazz.name}, 错误: ${e.message}", e)
        }
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
        val data = one.data ?: throw RuntimeException("道具数据内容为空，ID: ${one.id}")

        return try {
            val jsonObject: JSONObject = JSONUtil.parseObj(data)
            if (isInvalidCode(jsonObject.getStr("code")) && !isInvalidCode(one.code)) {
                jsonObject["code"] = one.code
            }
            if (isInvalidCode(jsonObject.getStr("kind")) && !one.kind.isNullOrBlank()) {
                jsonObject["kind"] = one.kind
            }

            val prop = JSONUtil.toBean(jsonObject.toString(), jsonConfig, clazz)
                ?: throw RuntimeException("道具反序列化结果为空")

            applyCodeIfPossible(prop, jsonObject.getStr("code") ?: one.code)
            prop
        } catch (e: Exception) {
            throw RuntimeException("道具反序列化失败，ID: ${one.id}, 目标类型: ${clazz.name}, 错误: ${e.message}", e)
        }
    }

    /**
     * 获取指定类型标识符对应的道具类
     *
     * @param kind 道具类型标识符
     * @return 对应的Class对象，不存在返回null
     */
    @JvmStatic
    fun getPropClass(kind: String): Class<out BaseProp>? = propsClassMap[kind]


    /**
     * 复制道具对象
     *
     * @param baseProp 原始道具实例
     * @return 复制后的道具实例
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    @Deprecated("准备弃用,请使用道具自己的clone方法")
    fun <T : BaseProp> copyProp(baseProp: T): T {
        val data = serialization(baseProp)
        return deserialization(data, baseProp.javaClass)
    }

    /**
     * 获取指定类型标识符对应的道具类
     *
     * todo 改成getPropClass
     * @param kind 道具类型标识符
     * @return 对应的Class对象，不存在返回null
     */
    @JvmStatic
    @Deprecated("准备弃用", replaceWith = ReplaceWith("getPropClass(kind)"))
    fun shopClass(kind: String): Class<out BaseProp>? = propsClassMap[kind]
}

