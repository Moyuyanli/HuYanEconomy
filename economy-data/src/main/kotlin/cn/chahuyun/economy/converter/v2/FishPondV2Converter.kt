package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.fish.FishPondEntity
import cn.chahuyun.economy.model.fish.FishPondDto

class FishPondV2Converter : Converter<FishPondEntity, FishPondDto> {

    override fun toDto(entity: FishPondEntity): FishPondDto {
        return FishPondDto(
            id = entity.id.toInt(),
            code = entity.code,
            admin = entity.admin,
            pondType = entity.pondType,
            name = entity.name,
            description = entity.description,
            pondLevel = entity.pondLevel,
            minLevel = entity.minLevel,
            rebate = entity.rebate,
            number = entity.number,
            fishCount = entity.fishCount
        )
    }

    override fun toEntity(dto: FishPondDto): FishPondEntity {
        val now = System.currentTimeMillis()
        return FishPondEntity(
            id = dto.id.toLong(),
            code = dto.code,
            admin = dto.admin,
            pondType = dto.pondType,
            name = dto.name,
            description = dto.description,
            pondLevel = dto.pondLevel,
            minLevel = dto.minLevel,
            rebate = dto.rebate,
            number = dto.number,
            fishCount = dto.fishCount,
            createdAt = now,
            updatedAt = now
        )
    }
}
