package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBankMainBankDebt
import cn.chahuyun.economy.model.privatebank.PrivateBankMainBankDebtDto
import java.util.*

class PrivateBankMainBankDebtV1Converter : Converter<PrivateBankMainBankDebt, PrivateBankMainBankDebtDto> {
    override fun toDto(entity: PrivateBankMainBankDebt) = PrivateBankMainBankDebtDto(
        id = entity.id,
        bankCode = entity.bankCode,
        principal = entity.principal,
        accruedInterest = entity.accruedInterest,
        lastAccruedAt = entity.lastAccruedAt.time,
        createdAt = entity.createdAt.time,
        updatedAt = entity.updatedAt.time,
        repaidAt = entity.repaidAt?.time ?: 0,
    )

    override fun toEntity(dto: PrivateBankMainBankDebtDto): PrivateBankMainBankDebt {
        val now = System.currentTimeMillis()
        return PrivateBankMainBankDebt(
            id = dto.id,
            bankCode = dto.bankCode,
            principal = dto.principal,
            accruedInterest = dto.accruedInterest,
            lastAccruedAt = Date(dto.lastAccruedAt.takeIf { it != 0L } ?: now),
            createdAt = Date(dto.createdAt.takeIf { it != 0L } ?: now),
            updatedAt = Date(dto.updatedAt.takeIf { it != 0L } ?: now),
            repaidAt = dto.repaidAt.takeIf { it != 0L }?.let(::Date),
        )
    }
}
