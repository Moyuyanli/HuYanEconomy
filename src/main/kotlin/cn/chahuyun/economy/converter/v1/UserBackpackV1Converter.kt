package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.UserBackpack
import cn.chahuyun.economy.model.user.UserBackpackDto

/**
 * UserBackpack V1实体与DTO转换器
 */
class UserBackpackV1Converter : Converter<UserBackpack, UserBackpackDto> {

    override fun toDto(entity: UserBackpack): UserBackpackDto {
        return UserBackpackDto(
            id = entity.id ?: 0,
            userId = entity.userId ?: "",
            propCode = entity.propCode ?: "",
            propKind = entity.propKind ?: "",
            propId = entity.propId ?: 0
        )
    }

    override fun toEntity(dto: UserBackpackDto): UserBackpack {
        return UserBackpack(
            id = if (dto.id != 0L) dto.id else null,
            userId = dto.userId.ifEmpty { null },
            propCode = dto.propCode.ifEmpty { null },
            propKind = dto.propKind.ifEmpty { null },
            propId = dto.propId
        )
    }
}
