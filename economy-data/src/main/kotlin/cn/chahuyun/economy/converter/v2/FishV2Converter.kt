package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.fish.FishEntity
import cn.chahuyun.economy.model.fish.FishDto

class FishV2Converter : Converter<FishEntity, FishDto> {

    override fun toDto(entity: FishEntity): FishDto {
        return FishDto(
            id = entity.id.toInt(),
            level = entity.level,
            name = entity.name,
            description = entity.description,
            price = entity.price,
            dimensionsMin = entity.dimensionsMin,
            dimensionsMax = entity.dimensionsMax,
            dimensions1 = entity.dimensions1,
            dimensions2 = entity.dimensions2,
            dimensions3 = entity.dimensions3,
            dimensions4 = entity.dimensions4,
            difficulty = entity.difficulty,
            special = entity.special
        )
    }

    override fun toEntity(dto: FishDto): FishEntity {
        val now = System.currentTimeMillis()
        return FishEntity(
            id = dto.id.toLong(),
            level = dto.level,
            name = dto.name,
            description = dto.description,
            price = dto.price,
            dimensionsMin = dto.dimensionsMin,
            dimensionsMax = dto.dimensionsMax,
            dimensions1 = dto.dimensions1,
            dimensions2 = dto.dimensions2,
            dimensions3 = dto.dimensions3,
            dimensions4 = dto.dimensions4,
            difficulty = dto.difficulty,
            special = dto.special,
            createdAt = now,
            updatedAt = now
        )
    }
}
