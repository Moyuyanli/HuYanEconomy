@file:Suppress("unused")

package cn.chahuyun.economy.prop

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.props.PropsDataDto
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.model.user.UserBackpackDto
import cn.chahuyun.economy.utils.Log
import cn.hutool.core.date.DateUtil
import cn.hutool.json.JSONConfig
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 道具管理器。
 *
 * 管理道具类型注册、模板注册、背包道具读取、持久化、使用和反序列化。
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

    private fun resolveExpiredTime(data: PropsDataDto, jsonObject: JSONObject?): Date? {
        if (data.expiredTime > 0) {
            return Date(data.expiredTime)
        }

        return when (val raw = jsonObject?.get("expiredTime")) {
            is Date -> raw
            is Number -> raw.toLong().takeIf { it > 0 }?.let { Date(it) }
            is String -> raw
                .takeIf { it.isNotBlank() && it != "null" }
                ?.let { text ->
                    text.toLongOrNull()?.let { Date(it) }
                        ?: runCatching { DateUtil.parse(text) }.getOrNull()
                }
            else -> null
        }
    }

    private fun applyPersistedFields(prop: BaseProp, data: PropsDataDto, jsonObject: JSONObject? = null) {
        if (prop is Stackable && data.num > 0) {
            prop.num = data.num
        }
        if (prop is Expirable) {
            prop.expiredTime = resolveExpiredTime(data, jsonObject) ?: prop.expiredTime
        }
        if (prop is cn.chahuyun.economy.model.props.PropsCard) {
            prop.status = data.status
        }
    }

    private val propsClassMap = ConcurrentHashMap<String, Class<out BaseProp>>()

    @PublishedApi
    internal val propsTemplateMap = ConcurrentHashMap<String, BaseProp>()

    /**
     * 注册道具类型和实现类的映射。
     *
     * @param kind 道具类型
     * @param propClass 道具实现类
     * @return 注册成功返回 true，类型已存在返回 false
     */
    @JvmStatic
    fun registerKindToPropClass(kind: String, propClass: Class<out BaseProp>): Boolean {
        if (propsClassMap.containsKey(kind)) return false
        propsClassMap[kind] = propClass
        return true
    }

    /**
     * 注册道具模板。
     *
     * 当前保持既有行为：同 code 模板会覆盖旧值，且不在此处自动加入商店。
     *
     * @param prop 道具模板
     * @param code 可选道具编码，null 时使用 prop.code
     * @return 类型未注册返回 false，否则返回 true
     */
    @JvmStatic
    fun registerCodeToProp(prop: BaseProp, code: String? = null): Boolean {
        if (!propsClassMap.containsKey(prop.kind)) {
            Log.debug("道具模板的类型未注册!")
            return false
        }
        val onlyCode = code ?: prop.code
        propsTemplateMap[onlyCode] = prop
        if (prop.canBuy) {
            PropsShop.addShop(onlyCode, prop)
        }
        return true
    }

    /**
     * 检查道具类型是否已经注册。
     *
     * @param kind 道具类型
     * @return 存在返回 true，否则返回 false
     */
    @JvmStatic
    fun checkKindExist(kind: String): Boolean = propsClassMap.containsKey(kind)

    /**
     * 检查道具模板 code 是否已经注册。
     *
     * @param code 道具 code
     * @return 存在返回 true，否则返回 false
     */
    @JvmStatic
    fun checkCodeExist(code: String): Boolean = propsTemplateMap.containsKey(code)

    /**
     * Kotlin 侧获取指定类型的道具模板副本。
     */
    inline fun <reified T : BaseProp> getTemplate(code: String): T {
        val prop = propsTemplateMap[code] ?: error("未找到道具模板 code: [$code]")
        return prop.copyProp()
    }

    /**
     * Java 侧获取指定类型的道具模板副本。
     */
    @JvmStatic
    @JvmName("getTemplate")
    fun <T : BaseProp> getTemplate(code: String, clazz: Class<T>): T {
        val prop =
            propsTemplateMap[code] ?: throw IllegalArgumentException("未找到道具模板 code: [$code]")
        if (!clazz.isInstance(prop)) {
            throw IllegalArgumentException("道具模板类型不符: code=$code, expected=${clazz.name}, actual=${prop.javaClass.name}")
        }
        return prop.copyProp()
    }

    /**
     * 根据背包记录读取道具实例。
     *
     * @param backpack 背包记录
     * @return 道具实例，读取失败返回 null
     */
    @JvmStatic
    fun getProp(backpack: UserBackpackDto): BaseProp? {
        return try {
            val clazz = propsClassMap[backpack.propKind] ?: return null
            val propId = backpack.propId.takeIf { it != 0L } ?: error("背包道具 id 缺失!")
            deserialization(propId, clazz)
        } catch (e: Exception) {
            Log.error("读取背包道具(ID: ${backpack.id})失败: ${e.message}")
            null
        }
    }

    /**
     * 根据背包记录读取指定类型的道具实例。
     */
    @JvmName("getPropWithType")
    inline fun <reified T : BaseProp> getProp(backpack: UserBackpackDto): T {
        return getProp(backpack, T::class.java)
    }

    /**
     * Java 侧根据背包记录读取指定类型的道具实例。
     */
    @JvmStatic
    fun <T : BaseProp> getProp(backpack: UserBackpackDto, clazz: Class<T>): T {
        val propId = backpack.propId.takeIf { it != 0L }
            ?: throw IllegalArgumentException("背包道具 id 缺失!")
        return deserialization(propId, clazz)
    }

    /**
     * 根据道具类型和实体 ID 读取道具实例。
     */
    @JvmStatic
    fun getProp(kind: String, id: Long): BaseProp? {
        val clazz = propsClassMap[kind] ?: return null
        return deserialization(id, clazz)
    }

    /**
     * 保存道具实例并返回持久化 ID。
     */
    @JvmStatic
    fun addProp(prop: BaseProp): Long {
        val data = serialization(prop)
        val saved = propsProxy.save(data)
        val propId = saved.id.takeIf { it != 0L }
            ?: error("保存道具失败: kind=${data.kind}, code=${data.code}")
        if (propsProxy.findById(propId) == null) {
            error("保存道具后读回失败: kind=${data.kind}, code=${data.code}, id=$propId")
        }
        return propId
    }

    /**
     * 更新指定 ID 的道具实例。
     */
    @JvmStatic
    fun updateProp(id: Long, prop: BaseProp) {
        // Backfill code from stored data when old serialized props lost it.
        if (isInvalidCode(prop.code)) {
            val existing = propsProxy.findById(id)
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
        val saved = propsProxy.save(data.copy(id = id))
        val verified = propsProxy.findById(id)
            ?: error("更新道具后读回失败: kind=${data.kind}, code=${data.code}, id=$id")
        if (
            saved.id == 0L ||
            verified.kind != data.kind ||
            verified.code != data.code ||
            verified.num != data.num ||
            verified.expiredTime != data.expiredTime ||
            verified.status != data.status ||
            verified.data != data.data
        ) {
            error("更新道具校验失败: kind=${data.kind}, code=${data.code}, id=$id")
        }
    }

    /**
     * Java 侧同步使用道具。
     */
    @JvmStatic
    fun usePropJava(backpack: UserBackpackDto, event: UseEvent) =
        runBlocking { return@runBlocking useProp(backpack, event) }

    /**
     * 异步使用道具。
     */
    suspend fun useProp(backpack: UserBackpackDto, event: UseEvent): UseResult {
        val prop = getProp(backpack) ?: return UseResult.fail("道具不存在")

        if (prop !is Usable) return UseResult.fail("该道具不可直接使用")

        val result = prop.use(event)

        if (result.success) {
            val propId = backpack.propId.takeIf { it != 0L } ?: error("背包道具 id 缺失!")
            if (result.shouldRemove) {
                destroyProsAndBackpack(propId)
                removeBackpackReference(event, propId)
            } else if (result.shouldUpdate) {
                if (prop is Stackable && prop.isConsumption) {
                    if (prop.num > 1) {
                        prop.num--
                        updateProp(propId, prop)
                    } else {
                        destroyProsAndBackpack(propId)
                        removeBackpackReference(event, propId)
                    }
                } else {
                    updateProp(propId, prop)
                }
            }
        }

        return result
    }

    private fun removeBackpackReference(event: UseEvent, propId: Long) {
        event.userInfo.backpacks = event.userInfo.backpacks.filterNot { it.propId == propId }
        event.userInfo.backpackCount = event.userInfo.backpacks.size
    }

    /**
     * 删除指定 ID 的道具实体。
     */
    @JvmStatic
    fun destroyPros(id: Long) {
        propsProxy.delete(id)
    }

    /**
     * 删除指定道具实体，并删除引用它的背包记录。
     */
    @JvmStatic
    fun destroyProsAndBackpack(propId: Long) {
        destroyPros(propId)
        backpackProxy.findWhere { it.propId == propId }.forEach { backpackProxy.delete(it.id) }
    }

    /**
     * 将道具实例序列化为持久化 DTO。
     */
    @JvmStatic
    fun serialization(prop: BaseProp): PropsDataDto {
        var expiredTime = 0L

        if (prop is Expirable && prop.canItExpire) {
            if (prop.expiredTime == null) {
                val days = if (prop.expireDays > 0) prop.expireDays else 1
                prop.expiredTime = DateUtil.offsetDay(Date(), days)
            }
            expiredTime = prop.expiredTime?.time ?: 0
        }

        var num = 1
        if (prop is Stackable) {
            num = prop.num
        }

        var status = false
        if (prop is cn.chahuyun.economy.model.props.PropsCard) {
            status = prop.status
        }

        return PropsDataDto(
            kind = prop.kind,
            code = prop.code,
            num = num,
            expiredTime = expiredTime,
            status = status,
            data = JSONUtil.toJsonStr(prop)
        )
    }

    /**
     * 根据持久化 ID 反序列化道具。
     */
    @JvmStatic
    fun <T : BaseProp> deserialization(id: Long, clazz: Class<T>): T {
        val propsData = propsProxy.findById(id)
            ?: throw RuntimeException("该道具数据不存在, ID: $id")

        val jsonConfig = JSONConfig.create().setIgnoreError(true)
        val data = propsData.data

        return try {
            val jsonObject = JSONUtil.parseObj(data)

            if (isInvalidCode(jsonObject.getStr("code")) && !isInvalidCode(propsData.code)) {
                jsonObject["code"] = propsData.code
            }
            if (isInvalidCode(jsonObject.getStr("kind")) && !propsData.kind.isNullOrBlank()) {
                jsonObject["kind"] = propsData.kind
            }

            val prop = JSONUtil.toBean(jsonObject.toString(), jsonConfig, clazz)
                ?: throw RuntimeException("道具反序列化结果为空")

            applyCodeIfPossible(prop, jsonObject.getStr("code") ?: propsData.code)
            applyPersistedFields(prop, propsData, jsonObject)
            prop
        } catch (e: Exception) {
            throw RuntimeException("道具反序列化失败，ID: $id, 目标类型: ${clazz.name}, 错误: ${e.message}", e)
        }
    }

    /**
     * 根据持久化 DTO 反序列化道具。
     */
    @JvmStatic
    fun <T : BaseProp> deserialization(one: PropsDataDto, clazz: Class<T>): T {
        val jsonConfig = JSONConfig.create().setIgnoreError(true)
        val data = one.data

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
            applyPersistedFields(prop, one, jsonObject)
            prop
        } catch (e: Exception) {
            throw RuntimeException("道具反序列化失败，ID: ${one.id}, 目标类型: ${clazz.name}, 错误: ${e.message}", e)
        }
    }

    /**
     * 获取指定类型对应的道具实现类。
     */
    @JvmStatic
    fun getPropClass(kind: String): Class<out BaseProp>? = propsClassMap[kind]

    fun listPropsData(): List<PropsDataDto> = propsProxy.findAll()

    fun savePropsData(data: PropsDataDto): PropsDataDto = propsProxy.save(data)

    private val propsProxy
        get() = EntityProxyRegistry.get<PropsDataDto>("props") ?: error("Props proxy is not initialized")

    private val backpackProxy
        get() = EntityProxyRegistry.get<UserBackpackDto>("user_backpack") ?: error("User backpack proxy is not initialized")
}
