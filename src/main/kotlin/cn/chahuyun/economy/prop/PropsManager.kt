@file:Suppress("unused")

package cn.chahuyun.economy.prop

import cn.chahuyun.economy.model.props.PropsDataDto
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.model.user.UserBackpackDto
import cn.chahuyun.economy.proxy.EntityProxyRegistry
import cn.chahuyun.economy.utils.Log
import cn.hutool.core.date.DateUtil
import cn.hutool.json.JSONConfig
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 閬撳叿绠＄悊鍣?(Kotlin 鐜颁唬閲嶆瀯鐗?
 * 瀹炵幇寮€闂師鍒欙紝鍩轰簬鎺ュ彛澶勭悊閬撳叿琛屼负
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
     * 娉ㄥ唽閬撳叿绫诲瀷鍒扮鐞嗗櫒
     *
     * @param kind 閬撳叿绫诲瀷鏍囪瘑绗?
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
     * 娉ㄥ唽浠ｇ爜鍒伴亾鍏锋槧灏勮〃涓?
     *
     * 濡傛灉璇ラ亾鍏峰彲浠ヨ喘涔?鍚屾娉ㄥ唽鍒板晢搴?
     *
     * @param prop 瑕佹敞鍐岀殑鍩虹閬撳叿瀵硅薄
     * @param code 瑕佹敞鍐岀殑閬撳叿浠ｇ爜锛屽鏋滀负null鍒欎娇鐢╬rop.code浣滀负榛樿鍊?
     * @return 娉ㄥ唽鎴愬姛杩斿洖true锛屽鏋滆浠ｇ爜宸插瓨鍦ㄥ垯杩斿洖false
     */
    @JvmStatic
    fun registerCodeToProp(prop: BaseProp, code: String? = null): Boolean {
        if (!propsClassMap.containsKey(prop.kind)) {
            Log.debug("閬撳叿妯℃澘鐨勭被鍨嬫湭娉ㄥ唽!")
            return false
        }
        val onlyCode = code ?: prop.code
        // 妫€鏌ラ亾鍏蜂唬鐮佹槸鍚﹀凡瀛樺湪锛屽瓨鍦ㄥ垯杩斿洖false
        if (propsTemplateMap.containsKey(onlyCode)) return false
        // 灏嗛亾鍏锋坊鍔犲埌妯℃澘鏄犲皠琛ㄤ腑
        propsTemplateMap[onlyCode] = prop
        // 濡傛灉閬撳叿鍙喘涔帮紝鍒欐坊鍔犲埌鍟嗗簵涓?
        if (prop.canBuy) PropsShop.addShop(onlyCode, prop)
        return true
    }


    /**
     * 妫€鏌ユ寚瀹氱殑閬撳叿绫诲瀷浠ｇ爜鏄惁瀛樺湪
     *
     * @param kind 閬撳叿绫诲瀷鏍囪瘑绗?
     * @return 瀛樺湪杩斿洖true锛屼笉瀛樺湪杩斿洖false
     */
    @JvmStatic
    fun checkKindExist(kind: String): Boolean = propsClassMap.containsKey(kind)

    /**
     * 妫€鏌ユ寚瀹氱殑閬撳叿code鏄惁瀛樺湪
     *
     * @param code 閬撳叿code鏍囪瘑绗?
     * @return 瀛樺湪杩斿洖true锛屼笉瀛樺湪杩斿洖false
     */
    @JvmStatic
    fun checkCodeExist(code: String): Boolean = propsTemplateMap.containsKey(code)

    /**
     * 銆怟otlin 涓撶敤銆戣幏鍙栭亾鍏锋ā鏉垮壇鏈?
     * 鍒╃敤 reified 鍏抽敭瀛楀疄鍖栨硾鍨嬶紝鏀寔杩愯鏃剁被鍨嬫鏌?
     * * 璋冪敤绀轰緥锛歷al weapon = PropManager.getTemplate<WeaponProp>("W001")
     */
    inline fun <reified T : BaseProp> getTemplate(code: String): T {
        val prop = propsTemplateMap[code] ?: error("鑾峰彇閬撳叿妯℃澘澶辫触, 閬撳叿 code: [$code] 涓嶅瓨鍦?")
        return prop.copyProp()
    }

    /**
     * 銆怞ava 涓撶敤銆戣幏鍙栭亾鍏锋ā鏉垮壇鏈?
     * 閫氳繃 Class 鏄惧紡浼犻€掔被鍨嬩俊鎭?
     * * 璋冪敤绀轰緥锛歐eaponProp w = PropManager.getTemplate("W001", WeaponProp.class)
     */
    @JvmStatic
    @JvmName("getTemplate") // 纭繚 Java 绔湅鍒扮殑鍚嶇О鏄?getTemplate
    fun <T : BaseProp> getTemplate(code: String, clazz: Class<T>): T {
        val prop =
            propsTemplateMap[code] ?: throw IllegalArgumentException("鑾峰彇閬撳叿妯℃澘澶辫触, 閬撳叿 code: [$code] 涓嶅瓨鍦?")
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
            val propId = backpack.propId.takeIf { it != 0L } ?: error("閿欒,鑳屽寘涓亾鍏穒d涓虹┖!")
            deserialization(propId, clazz)
        } catch (e: Exception) {
            Log.error("鑾峰彇鑳屽寘閬撳叿澶辫触(ID: ${backpack.id}), 鍙兘鏁版嵁宸叉崯鍧? ${e.message}")
            null
        }
    }

    /**
     * 鏍规嵁鐢ㄦ埛鑳屽寘淇℃伅鍜屾寚瀹氱被鍨嬭幏鍙栭亾鍏峰疄渚?
     *
     * @param backpack 鐢ㄦ埛鑳屽寘瀵硅薄
     * @return 杩斿洖鎸囧畾绫诲瀷鐨勯亾鍏峰疄渚?
     */
    @JvmName("getPropWithType")
    inline fun <reified T : BaseProp> getProp(backpack: UserBackpackDto): T {
        return getProp(backpack, T::class.java)
    }

    /**
     * 鏍规嵁鐢ㄦ埛鑳屽寘淇℃伅鍜屾寚瀹氱被鍨嬭幏鍙栭亾鍏峰疄渚?
     *
     * java 涓撶敤
     *
     * @param backpack 鐢ㄦ埛鑳屽寘瀵硅薄
     * @param clazz 閬撳叿绫诲瀷鐨凜lass瀵硅薄
     * @return 杩斿洖鎸囧畾绫诲瀷鐨勯亾鍏峰疄渚?
     */
    @JvmStatic
    fun <T : BaseProp> getProp(backpack: UserBackpackDto, clazz: Class<T>): T {
        // 妫€鏌ヨ儗鍖呬腑閬撳叿id鏄惁涓虹┖锛屼负绌哄垯鎶涘嚭寮傚父
        val propId = backpack.propId.takeIf { it != 0L } ?: throw IllegalArgumentException("鑳屽寘涓亾鍏穒d涓虹┖!")
        // 閫氳繃閬撳叿id鍜岀被鍨嬭繘琛屽弽搴忓垪鍖栬幏鍙栭亾鍏峰疄渚?
        return deserialization(propId, clazz)
    }


    /**
     * 鏍规嵁閬撳叿绫诲瀷鍜孖D鑾峰彇閬撳叿瀹炰緥
     *
     * @param kind 閬撳叿绫诲瀷鏍囪瘑绗?
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
        return propsProxy.save(data).id
    }

    /**
     * 鏇存柊鏁版嵁搴撲腑鐨勯亾鍏蜂俊鎭?
     *
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
        propsProxy.save(data.copy(id = id))
    }

    /**
     * Java鍏煎鐨勯亾鍏蜂娇鐢ㄦ柟娉曪紙鍚屾璋冪敤锛?
     *
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
            val propId = backpack.propId.takeIf { it != 0L } ?: error("閿欒,鑳屽寘涓亾鍏穒d涓虹┖!")
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
     * 鏍规嵁ID閿€姣侀亾鍏锋暟鎹?
     *
     * @param id 閬撳叿ID
     */
    @JvmStatic
    fun destroyPros(id: Long) {
        propsProxy.delete(id)
    }

    /**
     * 閿€姣佽儗鍖呬腑鐨勯亾鍏凤紙鍚屾椂鍒犻櫎閬撳叿鏁版嵁鍜岃儗鍖呰褰曪級
     *
     * @param propId 閬撳叿ID
     */
    @JvmStatic
    fun destroyProsAndBackpack(propId: Long) {
        destroyPros(propId)
        backpackProxy.findWhere { it.propId == propId }.forEach { backpackProxy.delete(it.id) }
    }

    /**
     * 鏍稿績瀛楁鍚屾搴忓垪鍖?
     * 灏嗛亾鍏峰璞″簭鍒楀寲涓烘暟鎹簱瀛樺偍鏍煎紡
     *
     * @param prop 閬撳叿瀹炰緥
     * @return 搴忓垪鍖栧悗鐨勯亾鍏锋暟鎹璞?
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
        // 澶勭悊鐘舵€佸瓧娈靛悓姝?(閽堝鍗＄墖绛?
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
        val data = propsData.data ?: throw RuntimeException("閬撳叿鏁版嵁鍐呭涓虹┖锛孖D: $id")

        return try {
            val jsonObject = JSONUtil.parseObj(data)

            // 浼樺厛浠モ€滃垪->JSON鈥濈殑鏂瑰紡琛ラ綈 code锛岄伩鍏?Kotlin val/鐖剁被瀛楁瀵艰嚧 toBean 涓㈠け code銆?
            if (isInvalidCode(jsonObject.getStr("code")) && !isInvalidCode(propsData.code)) {
                jsonObject["code"] = propsData.code
            }
            if (isInvalidCode(jsonObject.getStr("kind")) && !propsData.kind.isNullOrBlank()) {
                jsonObject["kind"] = propsData.kind
            }

            val prop = JSONUtil.toBean(jsonObject.toString(), jsonConfig, clazz)
                ?: throw RuntimeException("閬撳叿鍙嶅簭鍒楀寲缁撴灉涓虹┖")

            // 鏈€缁堝厹搴曪細濡傛灉 toBean 浠嶇劧鏈兘姝ｇ‘娉ㄥ叆 code锛屽垯浠?JSON/鍒楀洖濉埌瀵硅薄銆?
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
        val data = one.data ?: throw RuntimeException("閬撳叿鏁版嵁鍐呭涓虹┖锛孖D: ${one.id}")

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
     * 鑾峰彇鎸囧畾绫诲瀷鏍囪瘑绗﹀搴旂殑閬撳叿绫?
     *
     * @param kind 閬撳叿绫诲瀷鏍囪瘑绗?
     * @return 瀵瑰簲鐨凜lass瀵硅薄锛屼笉瀛樺湪杩斿洖null
     */
    @JvmStatic
    fun getPropClass(kind: String): Class<out BaseProp>? = propsClassMap[kind]


    /**
     * 澶嶅埗閬撳叿瀵硅薄
     *
     * @param baseProp 鍘熷閬撳叿瀹炰緥
     * @return 澶嶅埗鍚庣殑閬撳叿瀹炰緥
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    @Deprecated("鍑嗗寮冪敤,璇蜂娇鐢ㄩ亾鍏疯嚜宸辩殑clone鏂规硶")
    fun <T : BaseProp> copyProp(baseProp: T): T {
        val data = serialization(baseProp)
        return deserialization(data, baseProp.javaClass)
    }

    /**
     * 鑾峰彇鎸囧畾绫诲瀷鏍囪瘑绗﹀搴旂殑閬撳叿绫?
     *
     * todo 鏀规垚getPropClass
     * @param kind 閬撳叿绫诲瀷鏍囪瘑绗?
     * @return 瀵瑰簲鐨凜lass瀵硅薄锛屼笉瀛樺湪杩斿洖null
     */
    @JvmStatic
    @Deprecated("鍑嗗寮冪敤", replaceWith = ReplaceWith("getPropClass(kind)"))
    fun shopClass(kind: String): Class<out BaseProp>? = propsClassMap[kind]

    fun listPropsData(): List<PropsDataDto> = propsProxy.findAll()

    fun savePropsData(data: PropsDataDto): PropsDataDto = propsProxy.save(data)

    private val propsProxy
        get() = EntityProxyRegistry.get<PropsDataDto>("props") ?: error("Props proxy is not initialized")

    private val backpackProxy
        get() = EntityProxyRegistry.get<UserBackpackDto>("user_backpack") ?: error("User backpack proxy is not initialized")
}

