package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.GlobalFactorEntity
import cn.chahuyun.economy.model.GlobalFactorDto

class GlobalFactorV2Converter : Converter<GlobalFactorEntity, GlobalFactorDto> {

    override fun toDto(entity: GlobalFactorEntity): GlobalFactorDto {
        return GlobalFactorDto(
            id = entity.id.toInt(),
            robFactor = entity.robFactor,
            robBlankFactor = entity.robBlankFactor
        )
    }

    override fun toEntity(dto: GlobalFactorDto): GlobalFactorEntity {
        val now = System.currentTimeMillis()
        return GlobalFactorEntity(
            id = dto.id.toLong(),
            robFactor = dto.robFactor,
            robBlankFactor = dto.robBlankFactor,
            createdAt = now,
            updatedAt = now
        )
    }
}
