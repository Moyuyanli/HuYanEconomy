package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.props.PropsDataEntity
import cn.chahuyun.economy.model.props.PropsDataDto

class PropsDataV2Converter : Converter<PropsDataEntity, PropsDataDto> {

    override fun toDto(entity: PropsDataEntity): PropsDataDto {
        return PropsDataDto(
            id = entity.id,
            kind = entity.kind,
            code = entity.code,
            num = entity.num,
            expiredTime = entity.expiredTime,
            status = entity.status,
            data = entity.data
        )
    }

    override fun toEntity(dto: PropsDataDto): PropsDataEntity {
        val now = System.currentTimeMillis()
        return PropsDataEntity(
            id = dto.id,
            kind = dto.kind,
            code = dto.code,
            num = dto.num,
            expiredTime = dto.expiredTime,
            status = dto.status,
            data = dto.data,
            createdAt = now,
            updatedAt = now
        )
    }
}
