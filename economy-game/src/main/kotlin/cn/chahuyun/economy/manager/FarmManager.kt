package cn.chahuyun.economy.manager

import cn.chahuyun.economy.model.farm.FarmOperationResult
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.service.FarmOperationService
import cn.chahuyun.economy.service.FarmStateService
import cn.chahuyun.economy.service.FarmWaterService
import net.mamoe.mirai.contact.User
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object FarmManager {
    private val locks = ConcurrentHashMap<Long, ReentrantLock>()

    @JvmStatic
    fun init() {
        // Placeholder for future farm scheduled jobs.
    }

    fun <T> withUserLock(qq: Long, block: () -> T): T =
        locks.getOrPut(qq) { ReentrantLock() }.withLock(block)

    private fun <T> withTwoUserLocks(firstQq: Long, secondQq: Long, block: () -> T): T {
        if (firstQq == secondQq) return withUserLock(firstQq, block)
        val first = minOf(firstQq, secondQq)
        val second = maxOf(firstQq, secondQq)
        return locks.getOrPut(first) { ReentrantLock() }.withLock {
            locks.getOrPut(second) { ReentrantLock() }.withLock(block)
        }
    }

    fun buySeed(user: User, cropCode: String, amount: Int): FarmOperationResult = withUserLock(user.id) {
        FarmOperationService.buySeed(user, cropCode, amount, FarmStateService.getOrCreateFarm(user.id))
    }

    fun plant(user: User, plotNumbers: List<Int>, cropCode: String): FarmOperationResult = withUserLock(user.id) {
        FarmOperationService.plant(user, plotNumbers, cropCode, FarmStateService.getOrCreateFarm(user.id))
    }

    fun harvest(qq: Long, plotNumbers: List<Int>): FarmOperationResult = withUserLock(qq) {
        FarmOperationService.harvest(qq, plotNumbers, FarmStateService.getOrCreateFarm(qq))
    }

    fun sellFruits(user: User, cropCode: String?, amount: Int?): FarmOperationResult = withUserLock(user.id) {
        FarmOperationService.sellFruits(user, cropCode, amount)
    }

    fun upgradeFarm(user: User): FarmOperationResult = withUserLock(user.id) {
        FarmOperationService.upgradeFarm(user, FarmStateService.getOrCreateFarm(user.id))
    }

    fun activateShield(qq: Long): FarmOperationResult = withUserLock(qq) {
        FarmOperationService.activateShield(FarmStateService.getOrCreateFarm(qq))
    }

    fun water(waterer: UserInfoDto, targetQq: Long): FarmOperationResult = withTwoUserLocks(waterer.qq, targetQq) {
        FarmWaterService.water(
            waterer,
            FarmStateService.getOrCreateFarm(waterer.qq),
            FarmStateService.getOrCreateFarm(targetQq),
        )
    }
}
