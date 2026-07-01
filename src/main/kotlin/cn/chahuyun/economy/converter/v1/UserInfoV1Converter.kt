package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.model.user.UserInfoDto
import java.util.*

/**
 * UserInfo V1实体与DTO转换器
 */
class UserInfoV1Converter : Converter<UserInfo, UserInfoDto> {

    private val backpackConverter = UserBackpackV1Converter()

    override fun toDto(entity: UserInfo): UserInfoDto {
        return UserInfoDto(
            id = entity.id ?: "",
            qq = entity.qq,
            name = entity.name ?: "",
            registerGroup = entity.registerGroup,
            registerTime = entity.registerTime?.time ?: 0,
            sign = entity.sign,
            signTime = entity.signTime?.time ?: 0,
            signNumber = entity.signNumber,
            oldSignNumber = entity.oldSignNumber,
            signEarnings = entity.signEarnings,
            bankEarnings = entity.bankEarnings,
            defaultPrivateBankCode = entity.defaultPrivateBankCode ?: "",
            funding = entity.funding ?: "",
            backpackCount = entity.backpacks.size,
            backpacks = backpackConverter.toDtoList(entity.backpacks)
        )
    }

    override fun toEntity(dto: UserInfoDto): UserInfo {
        return UserInfo().apply {
            id = dto.id.ifEmpty { null }
            qq = dto.qq
            name = dto.name.ifEmpty { null }
            registerGroup = dto.registerGroup
            registerTime = dto.registerTime.takeIf { it > 0 }?.let { Date(it) }
            sign = dto.sign
            signTime = dto.signTime.takeIf { it > 0 }?.let { Date(it) }
            signNumber = dto.signNumber
            oldSignNumber = dto.oldSignNumber
            signEarnings = dto.signEarnings
            bankEarnings = dto.bankEarnings
            defaultPrivateBankCode = dto.defaultPrivateBankCode.ifEmpty { null }
            funding = dto.funding.ifEmpty { null }
        }
    }
}
