package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.fish.FishInfo
import cn.chahuyun.economy.model.fish.FishInfoDto

/**
 * FishInfo V1实体与DTO转换器
 */
class FishInfoV1Converter : Converter<FishInfo, FishInfoDto> {

    override fun toDto(entity: FishInfo): FishInfoDto {
        return FishInfoDto(
            id = entity.id,
            qq = entity.qq,
            isFishRod = entity.isFishRod,
            status = entity.status,
            rodLevel = entity.rodLevel,
            defaultFishPond = entity.defaultFishPond ?: ""
        )
    }

    override fun toEntity(dto: FishInfoDto): FishInfo {
        return FishInfo().apply {
            id = dto.id
            qq = dto.qq
            isFishRod = dto.isFishRod
            status = dto.status
            rodLevel = dto.rodLevel
            defaultFishPond = dto.defaultFishPond.ifEmpty { null }
        }
    }
}
