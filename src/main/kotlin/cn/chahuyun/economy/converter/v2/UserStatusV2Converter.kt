package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.user.UserStatusEntity
import cn.chahuyun.economy.model.user.UserStatusDto

class UserStatusV2Converter : Converter<UserStatusEntity, UserStatusDto> {

    override fun toDto(entity: UserStatusEntity): UserStatusDto {
        return UserStatusDto(
            id = entity.userId,
            place = entity.place,
            recoveryTime = entity.recoveryTime,
            startTime = entity.startTime
        )
    }

    override fun toEntity(dto: UserStatusDto): UserStatusEntity {
        val now = System.currentTimeMillis()
        return UserStatusEntity(
            userId = dto.id,
            place = dto.place,
            recoveryTime = dto.recoveryTime,
            startTime = dto.startTime,
            createdAt = now,
            updatedAt = now
        )
    }
}
