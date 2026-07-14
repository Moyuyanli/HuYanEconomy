package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.props.PropsData
import cn.chahuyun.economy.model.props.PropsDataDto
import java.util.*

/**
 * PropsData V1实体与DTO转换器
 */
class PropsDataV1Converter : Converter<PropsData, PropsDataDto> {

    override fun toDto(entity: PropsData): PropsDataDto {
        return PropsDataDto(
            id = entity.id ?: 0,
            kind = entity.kind ?: "",
            code = entity.code ?: "",
            num = entity.num,
            expiredTime = entity.expiredTime?.time ?: 0,
            status = entity.status,
            data = entity.data ?: ""
        )
    }

    override fun toEntity(dto: PropsDataDto): PropsData {
        return PropsData().apply {
            if (dto.id != 0L) id = dto.id
            kind = dto.kind.ifEmpty { null }
            code = dto.code.ifEmpty { null }
            num = dto.num
            expiredTime = dto.expiredTime.takeIf { it > 0 }?.let { Date(it) }
            status = dto.status
            data = dto.data.ifEmpty { null }
        }
    }
}
