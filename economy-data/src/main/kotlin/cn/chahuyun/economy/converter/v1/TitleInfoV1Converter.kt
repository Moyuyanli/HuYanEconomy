package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.TitleInfo
import cn.chahuyun.economy.model.user.TitleInfoDto
import java.util.*

/**
 * TitleInfo V1实体与DTO转换器
 */
class TitleInfoV1Converter : Converter<TitleInfo, TitleInfoDto> {

    override fun toDto(entity: TitleInfo): TitleInfoDto {
        return TitleInfoDto(
            id = entity.id,
            userId = entity.userId,
            code = entity.code ?: "",
            name = entity.name ?: "",
            status = entity.status,
            title = entity.title ?: "",
            impactName = entity.impactName,
            gradient = entity.gradient,
            sColor = entity.sColor ?: "",
            eColor = entity.eColor ?: "",
            dueTime = entity.dueTime?.time ?: 0
        )
    }

    override fun toEntity(dto: TitleInfoDto): TitleInfo {
        return TitleInfo().apply {
            id = dto.id
            userId = dto.userId
            code = dto.code.ifEmpty { null }
            name = dto.name.ifEmpty { null }
            status = dto.status
            title = dto.title.ifEmpty { null }
            impactName = dto.impactName
            gradient = dto.gradient
            sColor = dto.sColor.ifEmpty { null }
            eColor = dto.eColor.ifEmpty { null }
            dueTime = dto.dueTime.takeIf { it > 0 }?.let { Date(it) }
        }
    }
}
