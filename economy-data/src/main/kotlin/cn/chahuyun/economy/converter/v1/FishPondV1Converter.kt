package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.model.fish.FishPondDto

/**
 * FishPond V1实体与DTO转换器
 */
class FishPondV1Converter : Converter<FishPond, FishPondDto> {

    override fun toDto(entity: FishPond): FishPondDto {
        return FishPondDto(
            id = entity.id,
            code = entity.code,
            admin = entity.admin,
            pondType = entity.pondType,
            name = entity.name ?: "",
            description = entity.description ?: "",
            pondLevel = entity.pondLevel,
            minLevel = entity.minLevel,
            rebate = entity.rebate,
            number = entity.number,
            fishCount = entity.fishList?.size ?: 0
        )
    }

    override fun toEntity(dto: FishPondDto): FishPond {
        return FishPond().apply {
            id = dto.id
            code = dto.code
            admin = dto.admin
            pondType = dto.pondType
            name = dto.name.ifEmpty { null }
            description = dto.description.ifEmpty { null }
            pondLevel = dto.pondLevel
            minLevel = dto.minLevel
            rebate = dto.rebate
            number = dto.number
        }
    }
}
