package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.user.UserEntity
import cn.chahuyun.economy.model.user.UserInfoDto

/**
 * User V2 entity and DTO converter.
 */
class UserInfoV2Converter : Converter<UserEntity, UserInfoDto> {

    override fun toDto(entity: UserEntity): UserInfoDto {
        return UserInfoDto(
            id = entity.userKey ?: "",
            qq = entity.qq,
            name = entity.name,
            registerGroup = entity.registerGroup,
            registerTime = entity.registerTime,
            sign = entity.sign,
            signTime = entity.signTime,
            signNumber = entity.signNumber,
            oldSignNumber = entity.oldSignNumber,
            signEarnings = entity.signEarnings,
            bankEarnings = entity.bankEarnings,
            defaultPrivateBankCode = entity.defaultPrivateBankCode ?: "",
            funding = entity.funding ?: "",
            backpackCount = 0,
            backpacks = emptyList()
        )
    }

    override fun toEntity(dto: UserInfoDto): UserEntity {
        val now = System.currentTimeMillis()
        return UserEntity(
            userKey = dto.id.ifEmpty { null },
            qq = dto.qq,
            name = dto.name,
            registerGroup = dto.registerGroup,
            registerTime = dto.registerTime,
            sign = dto.sign,
            signTime = dto.signTime,
            signNumber = dto.signNumber,
            oldSignNumber = dto.oldSignNumber,
            signEarnings = dto.signEarnings,
            bankEarnings = dto.bankEarnings,
            defaultPrivateBankCode = dto.defaultPrivateBankCode.ifEmpty { null },
            funding = dto.funding.ifEmpty { null },
            createdAt = now,
            updatedAt = now
        )
    }
}
