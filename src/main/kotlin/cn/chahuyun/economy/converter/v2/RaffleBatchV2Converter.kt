package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.raffle.RaffleBatchEntity
import cn.chahuyun.economy.model.raffle.RaffleBatchDto
import cn.chahuyun.economy.prizes.RaffleResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RaffleBatchV2Converter : Converter<RaffleBatchEntity, RaffleBatchDto> {

    override fun toDto(entity: RaffleBatchEntity): RaffleBatchDto {
        val records = runCatching { Json.decodeFromString<List<RaffleResult>>(entity.records) }.getOrDefault(emptyList())
        return RaffleBatchDto(
            id = entity.id,
            userId = entity.userId,
            groupId = entity.groupId,
            poolId = entity.poolId,
            raffleType = entity.raffleType,
            createTime = entity.createTime,
            recordCount = if (entity.recordCount > 0) entity.recordCount else records.size,
            records = records
        )
    }

    override fun toEntity(dto: RaffleBatchDto): RaffleBatchEntity {
        val now = System.currentTimeMillis()
        return RaffleBatchEntity(
            id = dto.id,
            userId = dto.userId,
            groupId = dto.groupId,
            poolId = dto.poolId,
            raffleType = dto.raffleType,
            createTime = dto.createTime,
            recordCount = dto.recordCount.takeIf { it > 0 } ?: dto.records.size,
            records = Json.encodeToString(dto.records),
            createdAt = now,
            updatedAt = now
        )
    }
}
