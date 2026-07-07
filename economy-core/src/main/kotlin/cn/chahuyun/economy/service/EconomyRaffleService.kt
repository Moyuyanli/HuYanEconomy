package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.PrizesData
import cn.chahuyun.economy.model.user.UserRaffleDto
import cn.chahuyun.economy.prizes.PrizeHandle
import cn.chahuyun.economy.prizes.PrizesUtil.draw
import cn.chahuyun.economy.prizes.PrizesUtil.drawTen
import cn.chahuyun.economy.prizes.RaffleContext
import cn.chahuyun.economy.prizes.RaffleResult
import net.mamoe.mirai.contact.Group

/**
 * Core-facing raffle operations for feature modules.
 */
object EconomyRaffleService {

    @JvmStatic
    fun defaultPoolPrice(times: Int = 1): Double =
        defaultPool().price * times.toDouble()

    @JvmStatic
    fun drawSingle(userRaffle: UserRaffleDto, group: Group): EconomyRaffleDrawResult {
        val pool = defaultPool()
        val result = pool.draw(RaffleContext(pool, userRaffle, group))
        return EconomyRaffleDrawResult(listOf(result))
    }

    @JvmStatic
    fun drawTen(userRaffle: UserRaffleDto, group: Group): EconomyRaffleDrawResult {
        val pool = defaultPool()
        val results = pool.drawTen(RaffleContext(pool, userRaffle, group))
        return EconomyRaffleDrawResult(results)
    }

    @JvmStatic
    fun handle(result: EconomyRaffleDrawResult) {
        PrizeHandle.handle(result.rawResults)
    }

    private fun defaultPool() = PrizesData.pool.first()
}

class EconomyRaffleDrawResult internal constructor(
    internal val rawResults: List<RaffleResult>,
) {
    val prizes: List<EconomyRafflePrizeView> = rawResults.map {
        EconomyRafflePrizeView(it.prize.name, it.prize.description)
    }

    val firstPrize: EconomyRafflePrizeView
        get() = prizes.first()
}

data class EconomyRafflePrizeView(
    val name: String,
    val description: String,
)
