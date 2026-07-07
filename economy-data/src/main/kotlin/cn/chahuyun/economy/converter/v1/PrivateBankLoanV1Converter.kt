package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBankLoan
import cn.chahuyun.economy.model.privatebank.PrivateBankLoanDto
import java.util.*

/**
 * PrivateBankLoan V1实体与DTO转换器
 */
class PrivateBankLoanV1Converter : Converter<PrivateBankLoan, PrivateBankLoanDto> {

    override fun toDto(entity: PrivateBankLoan): PrivateBankLoanDto {
        return PrivateBankLoanDto(
            id = entity.id,
            offerId = entity.offerId,
            bankCode = entity.bankCode,
            lenderQq = entity.lenderQq,
            borrowerQq = entity.borrowerQq,
            principal = entity.principal,
            dueTotal = entity.dueTotal,
            repaidAmount = entity.repaidAmount,
            interest = entity.interest,
            termDays = entity.termDays,
            createdAt = entity.createdAt.time,
            dueAt = entity.dueAt.time,
            repaidAt = entity.repaidAt?.time ?: 0
        )
    }

    override fun toEntity(dto: PrivateBankLoanDto): PrivateBankLoan {
        return PrivateBankLoan().apply {
            id = dto.id
            offerId = dto.offerId
            bankCode = dto.bankCode
            lenderQq = dto.lenderQq
            borrowerQq = dto.borrowerQq
            principal = dto.principal
            dueTotal = dto.dueTotal
            repaidAmount = dto.repaidAmount
            interest = dto.interest
            termDays = dto.termDays
            createdAt = Date(dto.createdAt.takeIf { it != 0L } ?: Date().time)
            dueAt = Date(dto.dueAt.takeIf { it != 0L } ?: Date().time)
            repaidAt = dto.repaidAt.takeIf { it != 0L }?.let(::Date)
        }
    }
}
