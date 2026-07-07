package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.LotteryInfoDto

object LotteryDataService {

    fun findByType(type: Int): List<LotteryInfoDto> =
        lotteryProxy.findWhere { it.type == type }

    fun findAll(): List<LotteryInfoDto> =
        lotteryProxy.findAll()

    fun save(lotteryInfo: LotteryInfoDto): LotteryInfoDto =
        lotteryProxy.save(lotteryInfo)

    fun delete(lotteryInfo: LotteryInfoDto): Boolean =
        lotteryProxy.delete(lotteryInfo.id.toLong())

    private val lotteryProxy
        get() = EntityProxyRegistry.get<LotteryInfoDto>("lottery") ?: error("彩票代理器未初始化")
}
