package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.repository.FishRepository
import cn.chahuyun.economy.data.repository.FishingLogRepository
import cn.chahuyun.economy.data.repository.UserPondRepository
import cn.chahuyun.economy.entity.fishing.FishEntity
import cn.chahuyun.economy.entity.fishing.FishingLogEntity
import cn.chahuyun.economy.entity.fishing.UserPondEntity
import java.math.BigDecimal
import java.time.Instant

/**
 * 钓鱼流水（`hye_fishing_log`）服务。
 *
 * 流水是结算事实记录；排行榜、最大尺寸、最高收益等统计均从流水派生，
 * 不额外维护独立的排行榜表。
 */
object FishingLogService {

    /** 流水展示视图（已解析鱼名与鱼塘名快照）。 */
    data class FishingLogView(
        val fishName: String,
        val fishLevel: Int,
        val size: Int,
        val price: BigDecimal,
        val feeAmount: BigDecimal,
        val netIncome: BigDecimal,
        val pondName: String,
        val rodLevel: Int,
        val createdAt: Instant,
    )

    /** 写入一条钓鱼结算流水。 */
    fun record(
        userId: Long,
        fish: FishEntity,
        pond: UserPondEntity,
        rodLevel: Int,
        size: Int,
        price: BigDecimal,
        feeAmount: BigDecimal,
        netIncome: BigDecimal,
        baitItemId: Long?,
    ): FishingLogEntity = FishingLogRepository.save(
        FishingLogEntity(
            userId = userId,
            fishId = fish.id,
            pondId = pond.id,
            baitItemId = baitItemId,
            rodLevel = rodLevel,
            size = size,
            level = fish.level,
            price = price,
            feeAmount = feeAmount,
            netIncome = netIncome,
        )
    )

    fun recordsOf(userId: Long): List<FishingLogView> =
        FishingLogRepository.listByUser(userId).map { it.toView() }

    fun countOf(userId: Long): Int = FishingLogRepository.listByUser(userId).size

    fun pondCount(pondId: Long): Int = FishingLogRepository.listByPond(pondId).size

    /** 该用户钓到的最大尺寸。 */
    fun maxSizeOf(userId: Long): Int = recordsOf(userId).maxOfOrNull { it.size } ?: 0

    /** 该用户单条鱼最高收益。 */
    fun maxNetIncomeOf(userId: Long): BigDecimal =
        recordsOf(userId).maxOfOrNull { it.netIncome } ?: BigDecimal.ZERO

    /** 全服最大尺寸记录的持有者用户 ID，用于钓鱼榜首判定。 */
    fun globalTopUserId(): Long? =
        FishingLogRepository.listAll().maxByOrNull { it.size }?.userId

    private fun FishingLogEntity.toView(): FishingLogView {
        val fishName = FishRepository.findById(fishId)?.name ?: "未知鱼种"
        val pondName = UserPondRepository.findById(pondId)?.name ?: "未知鱼塘"
        return FishingLogView(
            fishName = fishName,
            fishLevel = level,
            size = size,
            price = price,
            feeAmount = feeAmount,
            netIncome = netIncome,
            pondName = pondName,
            rodLevel = rodLevel,
            createdAt = createdAt,
        )
    }
}
