package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.LotteryInfo
import cn.chahuyun.economy.model.LotteryInfoDto

/**
 * LotteryInfo V1实体与DTO转换器
 */
class LotteryInfoV1Converter : Converter<LotteryInfo, LotteryInfoDto> {

    override fun toDto(entity: LotteryInfo): LotteryInfoDto {
        return LotteryInfoDto(
            id = entity.id,
            qq = entity.qq,
            group = entity.group,
            money = entity.money,
            type = entity.type,
            number = entity.number ?: "",
            current = entity.current ?: "",
            bonus = entity.bonus
        )
    }

    override fun toEntity(dto: LotteryInfoDto): LotteryInfo {
        return LotteryInfo().apply {
            id = dto.id
            qq = dto.qq
            group = dto.group
            money = dto.money
            type = dto.type
            number = dto.number.ifEmpty { null }
            current = dto.current.ifEmpty { null }
            bonus = dto.bonus
        }
    }
}
