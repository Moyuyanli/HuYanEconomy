package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.constant.RaffleType
import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.raffle.RaffleBatch
import cn.chahuyun.economy.model.raffle.RaffleBatchDto
import java.util.*

class RaffleBatchV1Converter : Converter<RaffleBatch, RaffleBatchDto> {

    private val recordConverter = RaffleRecordV1Converter()

    override fun toDto(entity: RaffleBatch): RaffleBatchDto {
        return RaffleBatchDto(
            id = entity.id ?: 0,
            userId = entity.userId ?: 0,
            groupId = entity.groupId ?: 0,
            poolId = entity.poolId ?: "",
            raffleType = entity.raffleType?.name ?: "",
            createTime = entity.createTime?.time ?: 0,
            recordCount = entity.records.size,
            records = entity.records.map(recordConverter::toDto)
        )
    }

    override fun toEntity(dto: RaffleBatchDto): RaffleBatch {
        if (dto.records.isNotEmpty()) {
            return RaffleBatch(
                type = dto.raffleType.toRaffleType(),
                userId = dto.userId,
                groupId = dto.groupId,
                poolId = dto.poolId,
                records = dto.records
            ).apply {
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
}
