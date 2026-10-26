package cn.chahuyun.economy.games.farm

import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.service.EconomyAsyncService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.User
import java.util.concurrent.ConcurrentHashMap

/**
 * 农场玩法的唯一命令入口：参数解析 + 等级门槛 + 加锁执行。
 *
 * ## 为什么按用户加锁
 * 播种、收获、卖出都要读改写「农场状态 + 仓库 + 钱包」三处数据，同一用户并发两条指令
 * 会出现重复扣款或负库存。这里用 `qq -> Mutex` 保证单用户串行；跨用户玩法
 * （帮浇水、偷菜）同时锁两个用户，且**固定按 QQ 升序加锁**避免互相等待死锁。
 *
 * ## 为什么不再单独有 FarmManager
 * 原 `manager/FarmManager.kt` 只有「锁 + 转调 FarmOperationService」，唯一的外部调用方
 * 是主类里一个空的 `init()` 占位。按 Feature 垂直包规则，没有功能之外的调用方就不该留
 * Manager 壳，因此锁与委派并入本文件，`FarmUsecase` 直接调这里。
 *
 * 返回 [FarmOperationResult] 自带失败文案，动作层只负责把它回复出去。
 */
object FarmCommandService {

    /** 单用户锁表；按 QQ 懒创建，插件生命周期内常驻。 */
    private val locks = ConcurrentHashMap<Long, Mutex>()

    // ------------------------------------------------------------ 单用户指令

    /**
     * 购买种子：`购买种子 <作物> [数量]`。
     *
     * @param user 购买者。
     * @param raw 去掉指令前缀后的正文。
     */
    suspend fun buySeed(user: User, raw: String): FarmOperationResult {
        val command = FarmCommandParser.parseCropAmount(raw)
        val cropView = command.cropView
            ?: return FarmOperationResult(false, "作物不存在: ${command.rawCropName}")
        return withUserLock(user.id) {
            FarmOperationService.buySeed(user, cropView.code, command.amount, FarmStateService.getOrCreateFarm(user.id))
        }
    }

    /**
     * 播种：`播种|种植 <土地…> <作物>`。
     *
     * @param user 播种者。
     * @param raw 去掉指令前缀后的正文。
     */
    suspend fun plant(user: User, raw: String): FarmOperationResult {
        val command = FarmCommandParser.parsePlant(raw)
            ?: return FarmOperationResult(false, "格式: 播种 <土地编号...> <作物名>")
        val cropView = command.cropView
            ?: return FarmOperationResult(false, "作物不存在: ${command.rawCropName}")

        if (command.plotNumbers.isEmpty()) {
            return FarmOperationResult(false, "没有有效土地编号")
        }

        return plant(user, command.plotNumbers, cropView.code)
    }

    /**
     * 按土地编号播种（一键播种也走这里）。
     *
     * @param user 播种者。
     * @param plotNumbers 已解析的土地编号。
     * @param cropCode 作物编码。
     */
    suspend fun plant(user: User, plotNumbers: List<Int>, cropCode: String): FarmOperationResult =
        withUserLock(user.id) {
            FarmOperationService.plant(user, plotNumbers, cropCode, FarmStateService.getOrCreateFarm(user.id))
        }

    /**
     * 收获：`收获 <土地…>`。
     *
     * @param qq 收获者 QQ。
     * @param raw 去掉指令前缀后的正文。
     */
    suspend fun harvest(qq: Long, raw: String): FarmOperationResult {
        val plotNumbers = FarmCommandParser.parsePlotNumbers(raw)
        if (plotNumbers.isEmpty()) {
            return FarmOperationResult(false, "没有有效土地编号")
        }
        return harvest(qq, plotNumbers)
    }

    /**
     * 按土地编号收获（一键收获也走这里）。
     *
     * @param qq 收获者 QQ。
     * @param plotNumbers 已解析的土地编号。
     */
    suspend fun harvest(qq: Long, plotNumbers: List<Int>): FarmOperationResult =
        withUserLock(qq) {
            FarmOperationService.harvest(qq, plotNumbers, FarmStateService.getOrCreateFarm(qq))
        }

    /**
     * 卖出果实：`卖出果实 <作物|全部> [数量]`。
     *
     * @param user 卖出者。
     * @param raw 去掉指令前缀后的正文。
     */
    suspend fun sellFruits(user: User, raw: String): FarmOperationResult {
        val command = FarmCommandParser.parseSell(raw)
            ?: return FarmOperationResult(false, "格式: 卖出果实 <作物名|全部> [数量]")
        val cropView = command.cropView
        if (!command.sellAll && cropView == null) {
            return FarmOperationResult(false, "作物不存在: ${command.rawCropName}")
        }
        return sellFruits(user, cropView?.code, command.amount)
    }

    /**
     * 卖出指定作物或全部果实。
     *
     * @param user 卖出者。
     * @param cropCode 作物编码；null 表示全部卖出。
     * @param amount 数量；null 表示该作物全部。
     */
    suspend fun sellFruits(user: User, cropCode: String?, amount: Int?): FarmOperationResult =
        withUserLock(user.id) {
            FarmOperationService.sellFruits(user, cropCode, amount)
        }

    /**
     * 一键卖出（14 级开放）。
     *
     * @param user 卖出者。
     */
    suspend fun sellAll(user: User): FarmOperationResult {
        val state = FarmViewService.getOrCreateViewState(user.id)
        if (!state.hasLevel(14)) {
            return FarmOperationResult(false, "14级开放一键卖出")
        }
        return sellFruits(user, null, null)
    }

    /**
     * 一键收获（15 级开放）。
     *
     * @param qq 收获者 QQ。
     * @param now 判定成熟的当前时间戳，便于测试注入。
     */
    suspend fun harvestAll(qq: Long, now: Long = System.currentTimeMillis()): FarmOperationResult {
        val state = FarmViewService.getOrCreateViewState(qq)
        if (!state.hasLevel(15)) {
            return FarmOperationResult(false, "15级开放一键收获")
        }

        val plotNumbers = state.readyPlotNumbers(now)
        if (plotNumbers.isEmpty()) {
            return FarmOperationResult(false, "没有成熟作物可收获")
        }

        return harvest(qq, plotNumbers)
    }

    /**
     * 一键播种（16 级开放）。
     *
     * @param user 播种者。
     * @param rawCropName 作物的编码或名称。
     */
    suspend fun plantAll(user: User, rawCropName: String): FarmOperationResult {
        val cropView = FarmCommandParser.parseCropName(rawCropName)
            ?: return FarmOperationResult(false, "作物不存在: $rawCropName")

        val state = FarmViewService.getOrCreateViewState(user.id)
        if (!state.hasLevel(16)) {
            return FarmOperationResult(false, "16级开放一键播种")
        }

        val plotNumbers = state.emptyPlotNumbers()
        if (plotNumbers.isEmpty()) {
            return FarmOperationResult(false, "没有空闲土地可播种")
        }

        return plant(user, plotNumbers, cropView.code)
    }

    /**
     * 升级农场一次。
     *
     * @param user 农场主。
     */
    suspend fun upgradeFarm(user: User): FarmOperationResult =
        withUserLock(user.id) {
            FarmOperationService.upgradeFarm(user, FarmStateService.getOrCreateFarm(user.id))
        }

    /**
     * 逐级升级农场直到失败，返回每一步的结果汇总。
     *
     * @param user 农场主。
     */
    suspend fun upgradeFarmUntilFailure(user: User): String =
        withUserLock(user.id) {
            val state = FarmStateService.getOrCreateFarm(user.id)
            FarmUpgradeLoop.runUntilFailure {
                FarmOperationService.upgradeFarm(user, state).toUpgradeStepResult()
            }
        }

    /**
     * 激活农场守护（17 级开放）。
     *
     * @param user 农场主。
     */
    suspend fun activateShield(user: User): FarmOperationResult =
        withUserLock(user.id) {
            FarmOperationService.activateShield(user, FarmStateService.getOrCreateFarm(user.id))
        }

    // ------------------------------------------------------------ 跨用户指令

    /**
     * 帮他人浇水。
     *
     * @param waterer 浇水者资料（记录每日互动次数）。
     * @param targetQq 被浇水者 QQ。
     * @param times 浇水次数（≥1）。
     */
    suspend fun water(waterer: UserInfoDto, targetQq: Long, times: Int = 1): FarmOperationResult =
        withTwoUserLocks(waterer.qq, targetQq) {
            FarmWaterService.water(
                waterer,
                FarmStateService.getOrCreateFarm(waterer.qq),
                FarmStateService.getOrCreateFarm(targetQq),
                times,
            )
        }

    /**
     * 偷他人作物。
     *
     * @param thief 偷菜者资料（等级决定成功率与偷取量）。
     * @param targetQq 被偷者 QQ。
     */
    suspend fun steal(thief: UserInfoDto, targetQq: Long): FarmOperationResult =
        withTwoUserLocks(thief.qq, targetQq) {
            FarmStealService.steal(
                thief,
                FarmStateService.getOrCreateFarm(thief.qq),
                FarmStateService.getOrCreateFarm(targetQq),
            )
        }

    // ------------------------------------------------------------ 加锁基础设施

    /** 单用户串行执行。 */
    private suspend fun <T> withUserLock(qq: Long, block: () -> T): T {
        val mutex = locks.getOrPut(qq) { Mutex() }
        mutex.lock()
        return try {
            withContext(EconomyAsyncService.coroutineDispatcher()) { block() }
        } finally {
            mutex.unlock()
        }
    }

    /**
     * 双用户串行执行。
     *
     * 两个 QQ 相同时退化为单锁；不同时**按数值升序加锁**，保证任意两条指令的加锁顺序一致，
     * 不会出现「A 锁 1 等 2、B 锁 2 等 1」的死锁。
     *
     * @param firstQq 参与方之一。
     * @param secondQq 参与方之二。
     * @param dispatcher 执行上下文，测试可注入。
     */
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

    /** 把农场操作结果适配成逐级升级循环需要的步进结果。 */
    private fun FarmOperationResult.toUpgradeStepResult(): FarmUpgradeStep =
        FarmUpgradeStep(success, message)
}
