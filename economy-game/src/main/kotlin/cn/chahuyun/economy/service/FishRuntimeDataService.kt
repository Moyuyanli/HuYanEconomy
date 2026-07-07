package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.fish.FishPondDto
import cn.chahuyun.economy.runtime.EconomyRuntime
import net.mamoe.mirai.contact.Group

object FishRuntimeDataService {

    fun findOrCreatePond(group: Group): FishPondDto {
        val code = "g-${group.id}"
        return fishPondProxy.findByKey(code)
            ?: fishPondProxy.save(
                FishPondDto(
                    code = code,
                    admin = EconomyRuntime.config.owner,
                    pondType = 1,
                    name = "${group.name}鱼塘",
                    description = "一个天然形成的鱼塘，鱼情良好。",
                    pondLevel = 6
                )
            )
    }

    fun finishFishing(fishInfo: FishInfoDto) {
        fishInfo.status = false
        saveFishInfo(fishInfo)
    }

    fun saveFishInfo(fishInfo: FishInfoDto): FishInfoDto =
        fishInfoProxy.save(fishInfo).also { saved ->
            fishInfo.id = saved.id
            fishInfo.qq = saved.qq
            fishInfo.isFishRod = saved.isFishRod
            fishInfo.status = saved.status
            fishInfo.rodLevel = saved.rodLevel
            fishInfo.defaultFishPond = saved.defaultFishPond
        }

    fun incrementPondCount(pond: FishPondDto) {
        pond.number++
        saveFishPond(pond)
    }

    fun saveFishPond(pond: FishPondDto): FishPondDto =
        fishPondProxy.save(pond).also { saved ->
            pond.id = saved.id
            pond.code = saved.code
            pond.admin = saved.admin
            pond.pondType = saved.pondType
            pond.name = saved.name
            pond.description = saved.description
            pond.pondLevel = saved.pondLevel
            pond.minLevel = saved.minLevel
            pond.rebate = saved.rebate
            pond.number = saved.number
            pond.fishCount = saved.fishCount
        }

    private val fishInfoProxy
        get() = EntityProxyRegistry.get<FishInfoDto>("fish_info") ?: error("钓鱼信息代理器未初始化")

    private val fishPondProxy
        get() = EntityProxyRegistry.get<FishPondDto>("fish_pond") ?: error("鱼塘代理器未初始化")
}
