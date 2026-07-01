package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.model.fish.FishRankingDto

/**
 * FishRanking V1实体与DTO转换器
 */
class FishRankingV1Converter : Converter<FishRanking, FishRankingDto> {

    override fun toDto(entity: FishRanking): FishRankingDto {
        return FishRankingDto(
            id = entity.id,
            qq = entity.qq,
            name = entity.name ?: "",
            dimensions = entity.dimensions,
            money = entity.money,
            fishRodLevel = entity.fishRodLevel,
            date = entity.date?.time ?: 0,
            fishName = entity.fish?.name ?: "",
            fishPondName = entity.fishPond?.name ?: "",
            fishLevel = entity.fish?.level ?: 0,
            fishPondLevel = entity.fishPond?.pondLevel ?: 0
        )
    }

    override fun toEntity(dto: FishRankingDto): FishRanking {
        return FishRanking().apply {
            id = dto.id
            qq = dto.qq
            name = dto.name.ifEmpty { null }
            dimensions = dto.dimensions
            money = dto.money
            fishRodLevel = dto.fishRodLevel
        }
    }
}
