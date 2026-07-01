package cn.chahuyun.economy.model.fish

import cn.chahuyun.economy.plugin.FishManager
import cn.chahuyun.economy.proxy.EntityProxyRegistry
import cn.chahuyun.economy.utils.EconomyUtil
import kotlinx.serialization.Serializable
import java.util.regex.Pattern

/**
 * 鱼塘DTO
 */
@Serializable
data class FishPondDto(
    /** 记录ID */
    var id: Int = 0,
    /** 鱼塘编码 */
    var code: String = "",
    /** 管理员QQ */
    var admin: Long = 0,
    /** 鱼塘类型 */
    var pondType: Int = 0,
    /** 鱼塘名称 */
    var name: String = "",
    /** 鱼塘描述 */
    var description: String = "",
    /** 鱼塘等级 */
    var pondLevel: Int = 0,
    /** 最低钓鱼等级要求 */
    var minLevel: Int = 0,
    /** 返利率 */
    var rebate: Double = 0.05,
    /** 鱼塘容量 */
    var number: Int = 0,
    /** 鱼种数量 */
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
