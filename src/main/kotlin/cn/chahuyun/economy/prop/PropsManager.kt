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
 * 閬撳叿绠＄悊鍣?Kotlin 瀹炵幇銆? *
 * 璐熻矗閬撳叿妯℃澘娉ㄥ唽銆佸疄渚嬪簭鍒楀寲/鍙嶅簭鍒楀寲銆佽儗鍖呴亾鍏疯鍙栧拰浣跨敤鍚庣殑鏇存柊銆? */
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
     * 娉ㄥ唽閬撳叿绫诲瀷鍒扮鐞嗗櫒
     *
     * @param kind 閬撳叿绫诲瀷鏍囪瘑
     * @param propClass 閬撳叿绫荤殑Class瀵硅薄
     * @return 娉ㄥ唽鎴愬姛杩斿洖true锛屽鏋滆绫诲瀷宸插瓨鍦ㄥ垯杩斿洖false
     */
    @JvmStatic
    fun registerKindToPropClass(kind: String, propClass: Class<out BaseProp>): Boolean {
        if (propsClassMap.containsKey(kind)) return false
        propsClassMap[kind] = propClass
        return true
    }


    /**
     * 娉ㄥ唽閬撳叿妯℃澘鍒?code 鏄犲皠琛ㄣ€?     *
     * 鍙湁绫诲瀷宸叉敞鍐岀殑閬撳叿鎵嶅厑璁告敞鍐屾ā鏉裤€?     *
     * @param prop 瑕佹敞鍐岀殑鍩虹閬撳叿瀵硅薄
     * @param code 鍙€夋ā鏉?code锛屼负 null 鏃朵娇鐢?prop.code
     * @return 娉ㄥ唽鎴愬姛杩斿洖true锛屽鏋滆浠ｇ爜宸插瓨鍦ㄥ垯杩斿洖false
     */
    @JvmStatic
    fun registerCodeToProp(prop: BaseProp, code: String? = null): Boolean {
        if (!propsClassMap.containsKey(prop.kind)) {
            Log.debug("閬撳叿妯℃澘鐨勭被鍨嬫湭娉ㄥ唽!")
            return false
        }
        val onlyCode = code ?: prop.code
        // code 宸插瓨鍦ㄦ椂娉ㄥ唽澶辫触銆?        if (propsTemplateMap.containsKey(onlyCode)) return false
        // 灏嗛亾鍏锋坊鍔犲埌妯℃澘鏄犲皠琛ㄤ腑
        propsTemplateMap[onlyCode] = prop
        // 鍙喘涔伴亾鍏峰悓姝ュ姞鍏ュ晢搴椼€?        if (prop.canBuy) PropsShop.addShop(onlyCode, prop)
        return true
    }


    /**
     * 妫€鏌ラ亾鍏风被鍨嬫槸鍚﹀凡娉ㄥ唽銆?     *
     * @param kind 閬撳叿绫诲瀷鏍囪瘑
     * @return 瀛樺湪杩斿洖true锛屼笉瀛樺湪杩斿洖false
     */
    @JvmStatic
    fun checkKindExist(kind: String): Boolean = propsClassMap.containsKey(kind)

    /**
     * 妫€鏌ユ寚瀹?code 鏄惁宸叉敞鍐屾ā鏉裤€?     *
     * @param code 閬撳叿 code
     * @return 瀛樺湪杩斿洖true锛屼笉瀛樺湪杩斿洖false
     */
    @JvmStatic
    fun checkCodeExist(code: String): Boolean = propsTemplateMap.containsKey(code)

    /**
     * Kotlin 涓撶敤妯℃澘鑾峰彇鏂规硶銆?     * 浣跨敤 reified 鑷姩鎺ㄦ柇杩斿洖绫诲瀷銆?     * * 璋冪敤绀轰緥锛歷al weapon = PropManager.getTemplate<WeaponProp>("W001")
     */
    inline fun <reified T : BaseProp> getTemplate(code: String): T {
        val prop = propsTemplateMap[code] ?: error("鏈壘鍒伴亾鍏锋ā鏉?code: [$code]")
        return prop.copyProp()
    }

    /**
     * Java 涓撶敤妯℃澘鑾峰彇鏂规硶銆?     * 閫氳繃 Class 鍙傛暟鎸囧畾杩斿洖绫诲瀷銆?     * * 璋冪敤绀轰緥锛歐eaponProp w = PropManager.getTemplate("W001", WeaponProp.class)
     */
    @JvmStatic
    @JvmName("getTemplate") // 璁?Java 璋冪敤鍚嶄繚鎸佷负 getTemplate
    fun <T : BaseProp> getTemplate(code: String, clazz: Class<T>): T {
        val prop =
            propsTemplateMap[code] ?: throw IllegalArgumentException("鏈壘鍒伴亾鍏锋ā鏉?code: [$code]")
        if (!clazz.isInstance(prop)) {
            throw IllegalArgumentException("閬撳叿妯℃澘绫诲瀷涓嶇: code=$code, expected=${clazz.name}, actual=${prop.javaClass.name}")
        }
        return prop.copyProp()
    }


    /**
     * 鏍规嵁鐢ㄦ埛鑳屽寘淇℃伅鑾峰彇閬撳叿瀹炰緥
     *
     * @param backpack 鐢ㄦ埛鑳屽寘瀵硅薄
     * @return 鎴愬姛杩斿洖閬撳叿瀹炰緥锛屽け璐ヨ繑鍥瀗ull
     */
    @JvmStatic
    fun getProp(backpack: UserBackpackDto): BaseProp? {
        return try {
            val clazz = propsClassMap[backpack.propKind] ?: return null
            val propId = backpack.propId.takeIf { it != 0L } ?: error("鑳屽寘閬撳叿 id 缂哄け!")
            deserialization(propId, clazz)
        } catch (e: Exception) {
            Log.error("璇诲彇鑳屽寘閬撳叿(ID: ${backpack.id})澶辫触: ${e.message}")
            null
        }
    }

    /**
     * 鏍规嵁鑳屽寘淇℃伅鑾峰彇鎸囧畾绫诲瀷鐨勯亾鍏峰疄渚嬨€?     *
     * @param backpack 鐢ㄦ埛鑳屽寘瀵硅薄
     * @return 鎸囧畾绫诲瀷鐨勯亾鍏峰疄渚?     */
    @JvmName("getPropWithType")
    inline fun <reified T : BaseProp> getProp(backpack: UserBackpackDto): T {
        return getProp(backpack, T::class.java)
    }

    /**
     * 鏍规嵁鑳屽寘淇℃伅鑾峰彇鎸囧畾绫诲瀷鐨勯亾鍏峰疄渚嬨€?     *
     * java 涓撶敤
     *
     * @param backpack 鐢ㄦ埛鑳屽寘瀵硅薄
     * @param clazz 閬撳叿绫诲瀷鐨凜lass瀵硅薄
     * @return 鎸囧畾绫诲瀷鐨勯亾鍏峰疄渚?     */
    @JvmStatic
    fun <T : BaseProp> getProp(backpack: UserBackpackDto, clazz: Class<T>): T {
        val propId = backpack.propId.takeIf { it != 0L }
            ?: throw IllegalArgumentException("背包道具 id 缺失!")
        return deserialization(propId, clazz)
    }


    /**
     * 鏍规嵁閬撳叿绫诲瀷鍜孖D鑾峰彇閬撳叿瀹炰緥
     *
     * @param kind 閬撳叿绫诲瀷鏍囪瘑
     * @param id 閬撳叿ID
     * @return 鎴愬姛杩斿洖閬撳叿瀹炰緥锛屽け璐ヨ繑鍥瀗ull
     */
    @JvmStatic
    fun getProp(kind: String, id: Long): BaseProp? {
        val clazz = propsClassMap[kind] ?: return null
        return deserialization(id, clazz)
    }

    /**
     * 娣诲姞閬撳叿鍒版暟鎹簱
     *
     * @param prop 閬撳叿瀹炰緥
     * @return 杩斿洖鏂版坊鍔犻亾鍏风殑ID
     */
    @JvmStatic
    fun addProp(prop: BaseProp): Long {
        val data = serialization(prop)
        val saved = propsProxy.save(data)
        val propId = saved.id.takeIf { it != 0L }
            ?: error("淇濆瓨閬撳叿澶辫触: kind=${data.kind}, code=${data.code}")
        if (propsProxy.findById(propId) == null) {
            error("淇濆瓨閬撳叿鍚庤鍥炲け璐? kind=${data.kind}, code=${data.code}, id=$propId")
        }
        return propId
    }

    /**
     * 鏇存柊宸叉湁閬撳叿瀹炰緥銆?     *
     * @param id 閬撳叿ID
     * @param prop 閬撳叿瀹炰緥
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
            ?: error("鏇存柊閬撳叿鍚庤鍥炲け璐? kind=${data.kind}, code=${data.code}, id=$id")
        if (
            saved.id == 0L ||
            verified.kind != data.kind ||
            verified.code != data.code ||
            verified.num != data.num ||
            verified.expiredTime != data.expiredTime ||
            verified.status != data.status ||
            verified.data != data.data
        ) {
            error("鏇存柊閬撳叿鏍￠獙澶辫触: kind=${data.kind}, code=${data.code}, id=$id")
        }
    }

    /**
     * Java 璋冪敤鐨勫悓姝ラ亾鍏蜂娇鐢ㄥ叆鍙ｃ€?     *
     * @param backpack 鐢ㄦ埛鑳屽寘瀵硅薄
     * @param event 浣跨敤浜嬩欢瀵硅薄
     * @return 浣跨敤缁撴灉
     */

    @JvmStatic
    fun usePropJava(backpack: UserBackpackDto, event: UseEvent) =
        runBlocking { return@runBlocking useProp(backpack, event) }

    /**
     * 寮傛浣跨敤閬撳叿
     *
     * @param backpack 鐢ㄦ埛鑳屽寘瀵硅薄
     * @param event 浣跨敤浜嬩欢瀵硅薄
     * @return 浣跨敤缁撴灉
     */
    suspend fun useProp(backpack: UserBackpackDto, event: UseEvent): UseResult {
        val prop = getProp(backpack) ?: return UseResult.fail("道具不存在")

        if (prop !is Usable) return UseResult.fail("该道具不可直接使用")

        val result = prop.use(event)

        if (result.success) {
            val propId = backpack.propId.takeIf { it != 0L } ?: error("鑳屽寘閬撳叿 id 缂哄け!")
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
     * 鎸?ID 閿€姣侀亾鍏枫€?     *
     * @param id 閬撳叿ID
     */
    @JvmStatic
    fun destroyPros(id: Long) {
        propsProxy.delete(id)
    }

    /**
     * 閿€姣侀亾鍏凤紝骞舵竻鐞嗘墍鏈夊紩鐢ㄨ閬撳叿鐨勮儗鍖呰褰曘€?     *
     * @param propId 閬撳叿ID
     */
    @JvmStatic
    fun destroyProsAndBackpack(propId: Long) {
        destroyPros(propId)
        backpackProxy.findWhere { it.propId == propId }.forEach { backpackProxy.delete(it.id) }
    }

    /**
     * 搴忓垪鍖栭亾鍏峰疄渚嬨€?     * 灏嗛亾鍏峰璞″簭鍒楀寲涓烘暟鎹簱瀛樺偍鏍煎紡
     *
     * @param prop 閬撳叿瀹炰緥
     * @return 閬撳叿鏁版嵁 DTO
     */
    @JvmStatic
    fun serialization(prop: BaseProp): PropsDataDto {
        // 鏍稿績瀛楁鎻愬彇
        var expiredTime = 0L

        if (prop is Expirable && prop.canItExpire) {
            if (prop.expiredTime == null) {
                val days = if (prop.expireDays > 0) prop.expireDays else 1
                prop.expiredTime = DateUtil.offsetDay(Date(), days)
            }
            expiredTime = prop.expiredTime?.time ?: 0
        }

        var num = 1
        // 澶勭悊鍫嗗彔瀛楁鍚屾
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
     * 鏍规嵁ID鍙嶅簭鍒楀寲閬撳叿瀵硅薄
     *
     * @param id 閬撳叿ID
     * @param clazz 閬撳叿绫诲瀷鐨凜lass瀵硅薄
     * @return 鍙嶅簭鍒楀寲鍚庣殑閬撳叿瀹炰緥
     */
    @JvmStatic
    fun <T : BaseProp> deserialization(id: Long, clazz: Class<T>): T {
        val propsData = propsProxy.findById(id)
            ?: throw RuntimeException("璇ラ亾鍏锋暟鎹笉瀛樺湪, ID: $id")

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
                ?: throw RuntimeException("閬撳叿鍙嶅簭鍒楀寲缁撴灉涓虹┖")

            applyCodeIfPossible(prop, jsonObject.getStr("code") ?: propsData.code)
            prop
        } catch (e: Exception) {
            throw RuntimeException("閬撳叿鍙嶅簭鍒楀寲澶辫触锛孖D: $id, 鐩爣绫诲瀷: ${clazz.name}, 閿欒: ${e.message}", e)
        }
    }

    /**
     * 鏍规嵁閬撳叿鏁版嵁瀵硅薄鍙嶅簭鍒楀寲閬撳叿瀵硅薄
     *
     * @param one 閬撳叿鏁版嵁瀵硅薄
     * @param clazz 閬撳叿绫诲瀷鐨凜lass瀵硅薄
     * @return 鍙嶅簭鍒楀寲鍚庣殑閬撳叿瀹炰緥
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
                ?: throw RuntimeException("閬撳叿鍙嶅簭鍒楀寲缁撴灉涓虹┖")

            applyCodeIfPossible(prop, jsonObject.getStr("code") ?: one.code)
            prop
        } catch (e: Exception) {
            throw RuntimeException("閬撳叿鍙嶅簭鍒楀寲澶辫触锛孖D: ${one.id}, 鐩爣绫诲瀷: ${clazz.name}, 閿欒: ${e.message}", e)
        }
    }

    /**
     * 鑾峰彇鎸囧畾绫诲瀷鏍囪瘑绗﹀搴旂殑閬撳叿绫汇€?     *
     * @param kind 閬撳叿绫诲瀷鏍囪瘑绗?     * @return 瀵瑰簲鐨凜lass瀵硅薄锛屼笉瀛樺湪杩斿洖null
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

