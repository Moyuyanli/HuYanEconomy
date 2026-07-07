package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBankGovBondHolding
import cn.chahuyun.economy.model.privatebank.PrivateBankGovBondHoldingDto
import java.util.*

/**
 * PrivateBankGovBondHolding V1实体与DTO转换器
 */
class PrivateBankGovBondHoldingV1Converter : Converter<PrivateBankGovBondHolding, PrivateBankGovBondHoldingDto> {

    override fun toDto(entity: PrivateBankGovBondHolding): PrivateBankGovBondHoldingDto {
        return PrivateBankGovBondHoldingDto(
            id = entity.id,
            bankCode = entity.bankCode,
            issueId = entity.issueId,
            principal = entity.principal,
            rateMultiplier = entity.rateMultiplier,
            lockDays = entity.lockDays,
            boughtAt = entity.boughtAt.time,
            redeemedAt = entity.redeemedAt?.time ?: 0
        )
    }

    override fun toEntity(dto: PrivateBankGovBondHoldingDto): PrivateBankGovBondHolding {
        return PrivateBankGovBondHolding().apply {
            id = dto.id
            bankCode = dto.bankCode
            issueId = dto.issueId
            principal = dto.principal
            rateMultiplier = dto.rateMultiplier
            lockDays = dto.lockDays
            boughtAt = Date(dto.boughtAt.takeIf { it != 0L } ?: Date().time)
            redeemedAt = dto.redeemedAt.takeIf { it != 0L }?.let(::Date)
        }
    }
}
