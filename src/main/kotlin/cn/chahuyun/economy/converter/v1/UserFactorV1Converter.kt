package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.UserFactor
import cn.chahuyun.economy.model.user.UserFactorDto

/**
 * UserFactor V1实体与DTO转换器
 */
class UserFactorV1Converter : Converter<UserFactor, UserFactorDto> {

    override fun toDto(entity: UserFactor): UserFactorDto {
        return UserFactorDto(
            id = entity.id ?: 0,
            irritable = entity.irritable,
            force = entity.force,
            dodge = entity.dodge,
            resistance = entity.resistance,
            buff = entity.buff?.takeIf { it.isNotBlank() } ?: "[]"
        )
    }

    override fun toEntity(dto: UserFactorDto): UserFactor {
        return UserFactor().apply {
            if (dto.id != 0L) id = dto.id
            irritable = dto.irritable
            force = dto.force
            dodge = dto.dodge
            resistance = dto.resistance
            buff = dto.buff
        }
    }
}
