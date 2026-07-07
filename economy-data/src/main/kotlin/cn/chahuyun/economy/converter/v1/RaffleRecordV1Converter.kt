package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.raffle.RaffleRecord
import cn.chahuyun.economy.model.raffle.RaffleRecordDto

/**
 * RaffleRecord V1实体与DTO转换器
 */
class RaffleRecordV1Converter : Converter<RaffleRecord, RaffleRecordDto> {

    override fun toDto(entity: RaffleRecord): RaffleRecordDto {
        return RaffleRecordDto(
            id = entity.id ?: 0,
            batchId = entity.batch?.id ?: 0,
            prizeId = entity.prizeId ?: "",
            prizeName = entity.prizeName ?: "",
            level = entity.level ?: 0
        )
    }

    override fun toEntity(dto: RaffleRecordDto): RaffleRecord {
        return RaffleRecord().apply {
            if (dto.id != 0L) id = dto.id
            prizeId = dto.prizeId.ifEmpty { null }
            prizeName = dto.prizeName.ifEmpty { null }
            level = dto.level
        }
    }
}
