package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.constant.RaffleType
import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.data.PrizesData
import cn.chahuyun.economy.entity.raffle.RaffleBatch
import cn.chahuyun.economy.entity.raffle.RaffleRecord
import cn.chahuyun.economy.model.raffle.RaffleBatchDto
import cn.chahuyun.economy.prizes.Prize
import cn.chahuyun.economy.prizes.RaffleResult
import java.util.*

/**
 * RaffleBatch V1实体与DTO转换器
 */
class RaffleBatchV1Converter : Converter<RaffleBatch, RaffleBatchDto> {

    override fun toDto(entity: RaffleBatch): RaffleBatchDto {
        return RaffleBatchDto(
            id = entity.id ?: 0,
            userId = entity.userId ?: 0,
            groupId = entity.groupId ?: 0,
            poolId = entity.poolId ?: "",
            raffleType = entity.raffleType?.name ?: "",
            createTime = entity.createTime?.time ?: 0,
            recordCount = entity.records.size,
            records = entity.records.mapNotNull { it.toRaffleResult(entity) }
        )
    }

    override fun toEntity(dto: RaffleBatchDto): RaffleBatch {
        if (dto.records.isNotEmpty()) {
            return RaffleBatch(dto.raffleType.toRaffleType(), dto.records).apply {
                if (dto.id != 0L) id = dto.id
                createTime = dto.createTime.takeIf { it > 0 }?.let { Date(it) } ?: createTime
            }
        }

        return RaffleBatch().apply {
            if (dto.id != 0L) id = dto.id
            userId = dto.userId
            groupId = dto.groupId
            poolId = dto.poolId.ifEmpty { null }
            raffleType = dto.raffleType.takeIf { it.isNotBlank() }?.let { RaffleType.valueOf(it) }
            createTime = dto.createTime.takeIf { it > 0 }?.let { Date(it) }
        }
    }

    private fun String.toRaffleType(): RaffleType {
        return takeIf { it.isNotBlank() }?.let { RaffleType.valueOf(it) } ?: RaffleType.SINGLE
    }

    private fun RaffleRecord.toRaffleResult(batch: RaffleBatch): RaffleResult? {
        val pool = PrizesData.pool.find { it.id == batch.poolId } ?: return null
        val prize = runCatching { Prize.take(prizeId ?: "") }
            .getOrElse { Prize(prizeId ?: "", prizeName ?: "", "") }
        return RaffleResult(
            prize = prize,
            level = level ?: 0,
            groupId = batch.groupId ?: 0,
            userId = batch.userId ?: 0,
            pool = pool,
            timestamp = batch.createTime?.time ?: 0
        )
    }
}
