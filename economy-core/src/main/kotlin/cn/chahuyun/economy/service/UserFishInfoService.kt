package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.fish.FishInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto

object UserFishInfoService {

    fun findOrCreate(userInfo: UserInfoDto): FishInfoDto =
        fishInfoProxy.findById(userInfo.qq) ?: fishInfoProxy.save(
            FishInfoDto(
                id = userInfo.qq,
                qq = userInfo.qq,
                defaultFishPond = "g-${userInfo.registerGroup}"
            )
        )

    private val fishInfoProxy
        get() = EntityProxyRegistry.get<FishInfoDto>("fish_info") ?: error("钓鱼信息代理器未初始化")
}
