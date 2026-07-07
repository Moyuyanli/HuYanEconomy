package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.GlobalFactor
import cn.chahuyun.economy.model.GlobalFactorDto

/**
 * GlobalFactor V1实体与DTO转换器
 */
class GlobalFactorV1Converter : Converter<GlobalFactor, GlobalFactorDto> {

    override fun toDto(entity: GlobalFactor): GlobalFactorDto {
        return GlobalFactorDto(
            id = entity.id ?: 0,
            robFactor = entity.robFactor,
            robBlankFactor = entity.robBlankFactor
        )
    }

    override fun toEntity(dto: GlobalFactorDto): GlobalFactor {
        return GlobalFactor().apply {
            if (dto.id != 0) id = dto.id
            robFactor = dto.robFactor
            robBlankFactor = dto.robBlankFactor
        }
    }
}
