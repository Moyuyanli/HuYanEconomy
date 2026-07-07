package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.LotteryInfoEntity
import cn.chahuyun.economy.model.LotteryInfoDto

class LotteryInfoV2Converter : Converter<LotteryInfoEntity, LotteryInfoDto> {

    override fun toDto(entity: LotteryInfoEntity): LotteryInfoDto {
        return LotteryInfoDto(
            id = entity.id.toInt(),
            qq = entity.qq,
            group = entity.group,
            money = entity.money,
            type = entity.type,
            number = entity.number,
            current = entity.current,
            bonus = entity.bonus
        )
    }

    override fun toEntity(dto: LotteryInfoDto): LotteryInfoEntity {
        val now = System.currentTimeMillis()
        return LotteryInfoEntity(
            id = dto.id.toLong(),
            qq = dto.qq,
            group = dto.group,
            money = dto.money,
            type = dto.type,
            number = dto.number,
            current = dto.current,
            bonus = dto.bonus,
            createdAt = now,
            updatedAt = now
        )
    }
}
