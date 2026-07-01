package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.fish.FishInfoEntity
import cn.chahuyun.economy.model.fish.FishInfoDto

class FishInfoV2Converter : Converter<FishInfoEntity, FishInfoDto> {

    override fun toDto(entity: FishInfoEntity): FishInfoDto {
        return FishInfoDto(
            id = entity.qq,
            qq = entity.qq,
            isFishRod = entity.isFishRod,
            status = entity.status,
            rodLevel = entity.rodLevel,
            defaultFishPond = entity.defaultFishPond
        )
    }

    override fun toEntity(dto: FishInfoDto): FishInfoEntity {
        val now = System.currentTimeMillis()
        return FishInfoEntity(
            qq = dto.qq.takeIf { it != 0L } ?: dto.id,
            isFishRod = dto.isFishRod,
            status = dto.status,
            rodLevel = dto.rodLevel,
            defaultFishPond = dto.defaultFishPond,
            createdAt = now,
            updatedAt = now
        )
    }
}
