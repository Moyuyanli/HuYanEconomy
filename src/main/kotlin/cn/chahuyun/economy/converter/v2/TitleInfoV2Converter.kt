package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.title.TitleInfoEntity
import cn.chahuyun.economy.model.user.TitleInfoDto

class TitleInfoV2Converter : Converter<TitleInfoEntity, TitleInfoDto> {

    override fun toDto(entity: TitleInfoEntity): TitleInfoDto {
        return TitleInfoDto(
            id = entity.id.toInt(),
            userId = entity.userId,
            code = entity.code,
            name = entity.name,
            status = entity.status,
            title = entity.title,
            impactName = entity.impactName,
            gradient = entity.gradient,
            sColor = entity.sColor,
            eColor = entity.eColor,
            dueTime = entity.dueTime
        )
    }

    override fun toEntity(dto: TitleInfoDto): TitleInfoEntity {
        val now = System.currentTimeMillis()
        return TitleInfoEntity(
            id = dto.id.toLong(),
            userId = dto.userId,
            code = dto.code,
            name = dto.name,
            status = dto.status,
            title = dto.title,
            impactName = dto.impactName,
            gradient = dto.gradient,
            sColor = dto.sColor,
            eColor = dto.eColor,
            dueTime = dto.dueTime,
            createdAt = now,
            updatedAt = now
        )
    }
}
