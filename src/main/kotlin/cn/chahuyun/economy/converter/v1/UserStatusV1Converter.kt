package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.constant.UserLocation
import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.UserStatus
import cn.chahuyun.economy.model.user.UserStatusDto
import java.util.*

/**
 * UserStatus V1实体与DTO转换器
 */
class UserStatusV1Converter : Converter<UserStatus, UserStatusDto> {

    override fun toDto(entity: UserStatus): UserStatusDto {
        return UserStatusDto(
            id = entity.id ?: 0,
            place = entity.place.name,
            recoveryTime = entity.recoveryTime,
            startTime = entity.startTime?.time ?: 0
        )
    }

    override fun toEntity(dto: UserStatusDto): UserStatus {
        return UserStatus().apply {
            if (dto.id != 0L) id = dto.id
            place = try {
                UserLocation.valueOf(dto.place)
            } catch (e: Exception) {
                UserLocation.HOME
            }
            recoveryTime = dto.recoveryTime
            startTime = dto.startTime.takeIf { it > 0 }?.let { Date(it) }
        }
    }
}
