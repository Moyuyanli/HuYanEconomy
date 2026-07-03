package cn.chahuyun.economy.model.fish

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.plugin.FishManager
import cn.chahuyun.economy.utils.EconomyUtil
import kotlinx.serialization.Serializable
import java.util.regex.Pattern

/**
 * 楸煎DTO
 */
@Serializable
data class FishPondDto(
    /** 璁板綍ID */
    var id: Int = 0,
    /** 楸煎缂栫爜 */
    var code: String = "",
    /** 绠＄悊鍛楺Q */
    var admin: Long = 0,
    /** 楸煎绫诲瀷 */
    var pondType: Int = 0,
    /** 楸煎鍚嶇О */
    var name: String = "",
    /** 楸煎鎻忚堪 */
    var description: String = "",
    /** 楸煎绛夌骇 */
    var pondLevel: Int = 0,
    /** 鏈€浣庨挀楸肩瓑绾ц姹?*/
    var minLevel: Int = 0,
    /** 杩斿埄鐜?*/
    var rebate: Double = 0.05,
    /** 楸煎瀹归噺 */
    var number: Int = 0,
    /** 楸肩鏁伴噺 */
    var fishCount: Int = 0
) {
    val group: Long
        get() {
            val matcher = Pattern.compile("g-(\\d+)").matcher(code)
            return if (matcher.find()) matcher.group(1).toLong() else 0L
        }

    fun getFishPondMoney(): Double = EconomyUtil.getMoneyFromPluginBankForId(code, description)

    fun getFishList(level: Int): List<FishDto> {
        return if (pondType == 1) FishManager.getLevelFishList(level) else emptyList()
    }

    fun addNumber() {
        number++
        save()
    }

    fun save(): FishPondDto = fishPondProxy.save(this).also { saved ->
        id = saved.id
        code = saved.code
        admin = saved.admin
        pondType = saved.pondType
        name = saved.name
        description = saved.description
        pondLevel = saved.pondLevel
        minLevel = saved.minLevel
        rebate = saved.rebate
        number = saved.number
        fishCount = saved.fishCount
    }

    private val fishPondProxy
        get() = EntityProxyRegistry.get<FishPondDto>("fish_pond") ?: error("鱼塘代理器未初始化")
}
