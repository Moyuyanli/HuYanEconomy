package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBankLoanOffer
import cn.chahuyun.economy.model.privatebank.PrivateBankLoanOfferDto

/**
 * PrivateBankLoanOffer V1实体与DTO转换器
 */
class PrivateBankLoanOfferV1Converter : Converter<PrivateBankLoanOffer, PrivateBankLoanOfferDto> {

    override fun toDto(entity: PrivateBankLoanOffer): PrivateBankLoanOfferDto {
        return PrivateBankLoanOfferDto(
            id = entity.id,
            bankCode = entity.bankCode,
            ownerQq = entity.ownerQq,
            source = entity.source,
            total = entity.total,
            remaining = entity.remaining,
            interest = entity.interest,
            termDays = entity.termDays,
            enabled = entity.enabled,
            createdAt = entity.createdAt.time
        )
    }

    override fun toEntity(dto: PrivateBankLoanOfferDto): PrivateBankLoanOffer {
        return PrivateBankLoanOffer().apply {
            id = dto.id
            bankCode = dto.bankCode
            ownerQq = dto.ownerQq
            source = dto.source
            total = dto.total
            remaining = dto.remaining
            interest = dto.interest
            termDays = dto.termDays
            enabled = dto.enabled
        }
    }
}
