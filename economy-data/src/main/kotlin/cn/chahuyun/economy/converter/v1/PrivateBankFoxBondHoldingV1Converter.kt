package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBankFoxBondHolding
import cn.chahuyun.economy.model.privatebank.PrivateBankFoxBondHoldingDto
import java.util.*

/**
 * PrivateBankFoxBondHolding V1实体与DTO转换器
 */
class PrivateBankFoxBondHoldingV1Converter : Converter<PrivateBankFoxBondHolding, PrivateBankFoxBondHoldingDto> {

    override fun toDto(entity: PrivateBankFoxBondHolding): PrivateBankFoxBondHoldingDto {
        return PrivateBankFoxBondHoldingDto(
            id = entity.id,
            bondCode = entity.bondCode,
            bankCode = entity.bankCode,
            principal = entity.principal,
            rate = entity.rate,
            startedAt = entity.startedAt.time,
            dueAt = entity.dueAt.time,
            redeemedAt = entity.redeemedAt?.time ?: 0
        )
    }

    override fun toEntity(dto: PrivateBankFoxBondHoldingDto): PrivateBankFoxBondHolding {
        return PrivateBankFoxBondHolding().apply {
            id = dto.id
            bondCode = dto.bondCode
            bankCode = dto.bankCode
            principal = dto.principal
            rate = dto.rate
            startedAt = Date(dto.startedAt.takeIf { it != 0L } ?: Date().time)
            dueAt = Date(dto.dueAt.takeIf { it != 0L } ?: Date().time)
            redeemedAt = dto.redeemedAt.takeIf { it != 0L }?.let(::Date)
        }
    }
}
