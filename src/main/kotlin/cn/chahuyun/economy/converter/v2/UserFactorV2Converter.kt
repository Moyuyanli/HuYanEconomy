package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.user.UserFactorEntity
import cn.chahuyun.economy.model.user.UserFactorDto

class UserFactorV2Converter : Converter<UserFactorEntity, UserFactorDto> {

    override fun toDto(entity: UserFactorEntity): UserFactorDto {
        return UserFactorDto(
            id = entity.userId,
            irritable = entity.irritable,
            force = entity.force,
            dodge = entity.dodge,
            resistance = entity.resistance,
            buff = entity.buff?.takeIf { it.isNotBlank() } ?: "[]"
        )
    }

    override fun toEntity(dto: UserFactorDto): UserFactorEntity {
        val now = System.currentTimeMillis()
        return UserFactorEntity(
            userId = dto.id,
            irritable = dto.irritable,
            force = dto.force,
            dodge = dto.dodge,
            resistance = dto.resistance,
            buff = dto.buff,
            createdAt = now,
            updatedAt = now
        )
    }
}
