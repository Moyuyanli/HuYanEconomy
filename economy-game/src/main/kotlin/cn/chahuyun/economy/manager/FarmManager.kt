package cn.chahuyun.economy.manager

import cn.chahuyun.economy.model.farm.FarmOperationResult
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.service.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.User
import java.util.concurrent.ConcurrentHashMap

object FarmManager {
    private val locks = ConcurrentHashMap<Long, Mutex>()

    @JvmStatic
    fun init() {
        // Placeholder for future farm scheduled jobs.
    }

    private suspend fun <T> withUserLock(qq: Long, block: () -> T): T {
        val mutex = locks.getOrPut(qq) { Mutex() }
        mutex.lock()
        return try {
            withContext(EconomyAsyncService.coroutineDispatcher()) { block() }
        } finally {
            mutex.unlock()
        }
    }

    internal suspend fun <T> withTwoUserLocks(
        firstQq: Long,
        secondQq: Long,
        dispatcher: CoroutineDispatcher = EconomyAsyncService.coroutineDispatcher(),
        block: () -> T,
    ): T {
        if (firstQq == secondQq) return withUserLock(firstQq, block)
        val first = minOf(firstQq, secondQq)
        val second = maxOf(firstQq, secondQq)
        val firstMutex = locks.getOrPut(first) { Mutex() }
        val secondMutex = locks.getOrPut(second) { Mutex() }
        firstMutex.lock()
        return try {
            secondMutex.lock()
            try {
                withContext(dispatcher) { block() }
            } finally {
                secondMutex.unlock()
            }
        } finally {
            firstMutex.unlock()
        }
    }

    suspend fun buySeed(user: User, cropCode: String, amount: Int): FarmOperationResult = withUserLock(user.id) {
        FarmOperationService.buySeed(user, cropCode, amount, FarmStateService.getOrCreateFarm(user.id))
    }

    suspend fun plant(user: User, plotNumbers: List<Int>, cropCode: String): FarmOperationResult = withUserLock(user.id) {
        FarmOperationService.plant(user, plotNumbers, cropCode, FarmStateService.getOrCreateFarm(user.id))
    }

    suspend fun harvest(qq: Long, plotNumbers: List<Int>): FarmOperationResult = withUserLock(qq) {
        FarmOperationService.harvest(qq, plotNumbers, FarmStateService.getOrCreateFarm(qq))
    }

    suspend fun sellFruits(user: User, cropCode: String?, amount: Int?): FarmOperationResult = withUserLock(user.id) {
        FarmOperationService.sellFruits(user, cropCode, amount)
    }

    suspend fun upgradeFarm(user: User): FarmOperationResult = withUserLock(user.id) {
        FarmOperationService.upgradeFarm(user, FarmStateService.getOrCreateFarm(user.id))
    }

    suspend fun upgradeFarmUntilFailure(user: User): String = withUserLock(user.id) {
        val state = FarmStateService.getOrCreateFarm(user.id)
        UpgradeLoopService.runUntilFailure {
            FarmOperationService.upgradeFarm(user, state).toUpgradeStepResult()
        }
    }

    suspend fun activateShield(user: User): FarmOperationResult = withUserLock(user.id) {
        FarmOperationService.activateShield(user, FarmStateService.getOrCreateFarm(user.id))
    }

    suspend fun water(waterer: UserInfoDto, targetQq: Long, times: Int = 1): FarmOperationResult = withTwoUserLocks(waterer.qq, targetQq) {
        FarmWaterService.water(
            waterer,
            FarmStateService.getOrCreateFarm(waterer.qq),
            FarmStateService.getOrCreateFarm(targetQq),
            times,
        )
    }

    suspend fun steal(thief: UserInfoDto, targetQq: Long): FarmOperationResult = withTwoUserLocks(thief.qq, targetQq) {
        FarmStealService.steal(
            thief,
            FarmStateService.getOrCreateFarm(thief.qq),
            FarmStateService.getOrCreateFarm(targetQq),
        )
    }

    private fun FarmOperationResult.toUpgradeStepResult(): UpgradeStepResult =
        UpgradeStepResult(success, message)
}
