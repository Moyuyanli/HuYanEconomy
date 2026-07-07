package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.user.UserBackpackEntity
import cn.chahuyun.economy.model.user.UserBackpackDto

/**
 * User backpack V2 entity and DTO converter.
 */
class UserBackpackV2Converter : Converter<UserBackpackEntity, UserBackpackDto> {

    override fun toDto(entity: UserBackpackEntity): UserBackpackDto {
        return UserBackpackDto(
            id = entity.id,
            userId = entity.userKey,
            propCode = entity.propCode,
            propKind = entity.propKind,
            propId = entity.propId
        )
    }

    override fun toEntity(dto: UserBackpackDto): UserBackpackEntity {
        val now = System.currentTimeMillis()
        return UserBackpackEntity(
            id = dto.id,
            userKey = dto.userId,
            propCode = dto.propCode,
            propKind = dto.propKind,
            propId = dto.propId,
            createdAt = now,
            updatedAt = now
        )
    }
}
