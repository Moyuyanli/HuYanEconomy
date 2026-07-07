package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.UserProperty
import cn.chahuyun.economy.model.user.UserPropertyDto

/**
 * UserProperty V1实体与DTO转换器
 */
class UserPropertyV1Converter : Converter<UserProperty, UserPropertyDto> {

    override fun toDto(entity: UserProperty): UserPropertyDto {
        return UserPropertyDto(
            id = entity.id ?: 0
        )
    }

    override fun toEntity(dto: UserPropertyDto): UserProperty {
        return UserProperty().apply {
            if (dto.id != 0L) id = dto.id
        }
    }
}
