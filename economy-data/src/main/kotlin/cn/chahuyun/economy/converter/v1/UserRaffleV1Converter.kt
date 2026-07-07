package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.UserRaffle
import cn.chahuyun.economy.model.user.UserRaffleDto

/**
 * UserRaffle V1实体与DTO转换器
 */
class UserRaffleV1Converter : Converter<UserRaffle, UserRaffleDto> {

    override fun toDto(entity: UserRaffle): UserRaffleDto {
        return UserRaffleDto(
            id = entity.id ?: 0,
            defaultPool = entity.defaultPool ?: "",
            times = entity.times ?: 0,
            jackpot = entity.jackpot ?: 0,
            poolTimes = entity.poolTimes.toMap()
        )
    }

    override fun toEntity(dto: UserRaffleDto): UserRaffle {
        return UserRaffle().apply {
            if (dto.id != 0L) id = dto.id
            defaultPool = dto.defaultPool.ifEmpty { null }
            times = dto.times
            jackpot = dto.jackpot
            poolTimes.clear()
            poolTimes.putAll(dto.poolTimes)
        }
    }
}
