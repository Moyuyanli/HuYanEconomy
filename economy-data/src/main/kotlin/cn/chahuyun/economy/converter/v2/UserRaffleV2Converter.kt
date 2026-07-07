package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.user.UserRaffleEntity
import cn.chahuyun.economy.model.user.UserRaffleDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserRaffleV2Converter : Converter<UserRaffleEntity, UserRaffleDto> {

    override fun toDto(entity: UserRaffleEntity): UserRaffleDto {
        return UserRaffleDto(
            id = entity.userId,
            defaultPool = entity.defaultPool,
            times = entity.times,
            jackpot = entity.jackpot,
            poolTimes = runCatching { Json.decodeFromString<Map<String, Int>>(entity.poolTimes) }.getOrDefault(emptyMap())
        )
    }

    override fun toEntity(dto: UserRaffleDto): UserRaffleEntity {
        val now = System.currentTimeMillis()
        return UserRaffleEntity(
            userId = dto.id,
            defaultPool = dto.defaultPool,
            times = dto.times,
            jackpot = dto.jackpot,
            poolTimes = Json.encodeToString(dto.poolTimes),
            createdAt = now,
            updatedAt = now
        )
    }
}
