package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.fish.Fish
import cn.chahuyun.economy.model.fish.FishDto

/**
 * Fish V1实体与DTO转换器
 */
class FishV1Converter : Converter<Fish, FishDto> {

    override fun toDto(entity: Fish): FishDto {
        return FishDto(
            id = entity.id,
            level = entity.level,
            name = entity.name ?: "",
            description = entity.description ?: "",
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

    override fun toEntity(dto: FishDto): Fish {
        return Fish().apply {
            id = dto.id
            level = dto.level
            name = dto.name.ifEmpty { null }
            description = dto.description.ifEmpty { null }
            price = dto.price
            dimensionsMin = dto.dimensionsMin
            dimensionsMax = dto.dimensionsMax
            dimensions1 = dto.dimensions1
            dimensions2 = dto.dimensions2
            dimensions3 = dto.dimensions3
            dimensions4 = dto.dimensions4
            difficulty = dto.difficulty
            special = dto.special
        }
    }
}
