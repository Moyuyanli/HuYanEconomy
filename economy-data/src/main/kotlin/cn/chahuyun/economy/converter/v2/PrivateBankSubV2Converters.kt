package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.privatebank.*
import cn.chahuyun.economy.model.privatebank.*

class PrivateBankDepositV2Converter : Converter<PrivateBankDepositEntity, PrivateBankDepositDto> {
    override fun toDto(entity: PrivateBankDepositEntity) = PrivateBankDepositDto(entity.id.toInt(), entity.bankCode, entity.userQq, entity.principal, entity.createdAt, entity.updatedAt)
    override fun toEntity(dto: PrivateBankDepositDto): PrivateBankDepositEntity {
        val now = System.currentTimeMillis()
        return PrivateBankDepositEntity(dto.id.toLong(), dto.bankCode, dto.userQq, dto.principal, dto.createdAt.takeIf { it != 0L } ?: now, dto.updatedAt.takeIf { it != 0L } ?: now)
    }
}

class PrivateBankReviewV2Converter : Converter<PrivateBankReviewEntity, PrivateBankReviewDto> {
    override fun toDto(entity: PrivateBankReviewEntity) = PrivateBankReviewDto(entity.id.toInt(), entity.bankCode, entity.userQq, entity.rating, entity.content, entity.createdAt)
    override fun toEntity(dto: PrivateBankReviewDto): PrivateBankReviewEntity {
        val now = System.currentTimeMillis()
        return PrivateBankReviewEntity(dto.id.toLong(), dto.bankCode, dto.userQq, dto.rating, dto.content, dto.createdAt.takeIf { it != 0L } ?: now)
    }
}

class PrivateBankGovBondIssueV2Converter : Converter<PrivateBankGovBondIssueEntity, PrivateBankGovBondIssueDto> {
    override fun toDto(entity: PrivateBankGovBondIssueEntity) = PrivateBankGovBondIssueDto(entity.id.toInt(), entity.weekKey, entity.rateMultiplier, entity.lockDays, entity.totalLimit, entity.remaining, entity.createdAt)
    override fun toEntity(dto: PrivateBankGovBondIssueDto): PrivateBankGovBondIssueEntity {
        val now = System.currentTimeMillis()
        return PrivateBankGovBondIssueEntity(dto.id.toLong(), dto.weekKey, dto.rateMultiplier, dto.lockDays, dto.totalLimit, dto.remaining, dto.createdAt.takeIf { it != 0L } ?: now)
    }
}

class PrivateBankGovBondHoldingV2Converter : Converter<PrivateBankGovBondHoldingEntity, PrivateBankGovBondHoldingDto> {
    override fun toDto(entity: PrivateBankGovBondHoldingEntity) = PrivateBankGovBondHoldingDto(entity.id.toInt(), entity.bankCode, entity.issueId, entity.principal, entity.rateMultiplier, entity.lockDays, entity.boughtAt, entity.redeemedAt)
    override fun toEntity(dto: PrivateBankGovBondHoldingDto) = PrivateBankGovBondHoldingEntity(dto.id.toLong(), dto.bankCode, dto.issueId, dto.principal, dto.rateMultiplier, dto.lockDays, dto.boughtAt.takeIf { it != 0L } ?: System.currentTimeMillis(), dto.redeemedAt)
}

class PrivateBankLoanOfferV2Converter : Converter<PrivateBankLoanOfferEntity, PrivateBankLoanOfferDto> {
    override fun toDto(entity: PrivateBankLoanOfferEntity) = PrivateBankLoanOfferDto(entity.id.toInt(), entity.bankCode, entity.ownerQq, entity.source, entity.total, entity.remaining, entity.interest, entity.termDays, entity.enabled, entity.createdAt)
    override fun toEntity(dto: PrivateBankLoanOfferDto): PrivateBankLoanOfferEntity {
        val now = System.currentTimeMillis()
        return PrivateBankLoanOfferEntity(dto.id.toLong(), dto.bankCode, dto.ownerQq, dto.source, dto.total, dto.remaining, dto.interest, dto.termDays, dto.enabled, dto.createdAt.takeIf { it != 0L } ?: now)
    }
}

class PrivateBankLoanV2Converter : Converter<PrivateBankLoanEntity, PrivateBankLoanDto> {
    override fun toDto(entity: PrivateBankLoanEntity) = PrivateBankLoanDto(entity.id.toInt(), entity.offerId, entity.bankCode, entity.lenderQq, entity.borrowerQq, entity.principal, entity.dueTotal, entity.repaidAmount, entity.interest, entity.termDays, entity.createdAt, entity.dueAt, entity.repaidAt)
    override fun toEntity(dto: PrivateBankLoanDto): PrivateBankLoanEntity {
        val now = System.currentTimeMillis()
        return PrivateBankLoanEntity(dto.id.toLong(), dto.offerId, dto.bankCode, dto.lenderQq, dto.borrowerQq, dto.principal, dto.dueTotal, dto.repaidAmount, dto.interest, dto.termDays, dto.createdAt.takeIf { it != 0L } ?: now, dto.dueAt.takeIf { it != 0L } ?: now, dto.repaidAt)
    }
}

class PrivateBankFoxBondV2Converter : Converter<PrivateBankFoxBondEntity, PrivateBankFoxBondDto> {
    override fun toDto(entity: PrivateBankFoxBondEntity) = PrivateBankFoxBondDto(entity.id.toInt(), entity.code, entity.faceValue, entity.baseRate, entity.termDays, entity.bidStartAt, entity.bidEndAt, entity.status, entity.winnerBankCode, entity.winnerBidRate, entity.winnerPremium, entity.createdAt)
    override fun toEntity(dto: PrivateBankFoxBondDto): PrivateBankFoxBondEntity {
        val now = System.currentTimeMillis()
        return PrivateBankFoxBondEntity(dto.id.toLong(), dto.code, dto.faceValue, dto.baseRate, dto.termDays, dto.bidStartAt.takeIf { it != 0L } ?: now, dto.bidEndAt.takeIf { it != 0L } ?: now, dto.status, dto.winnerBankCode, dto.winnerBidRate, dto.winnerPremium, dto.createdAt.takeIf { it != 0L } ?: now)
    }
}

class PrivateBankFoxBondBidV2Converter : Converter<PrivateBankFoxBondBidEntity, PrivateBankFoxBondBidDto> {
    override fun toDto(entity: PrivateBankFoxBondBidEntity) = PrivateBankFoxBondBidDto(entity.id.toInt(), entity.bondCode, entity.bankCode, entity.ownerQq, entity.premium, entity.bidRate, entity.createdAt)
    override fun toEntity(dto: PrivateBankFoxBondBidDto): PrivateBankFoxBondBidEntity {
        val now = System.currentTimeMillis()
        return PrivateBankFoxBondBidEntity(dto.id.toLong(), dto.bondCode, dto.bankCode, dto.ownerQq, dto.premium, dto.bidRate, dto.createdAt.takeIf { it != 0L } ?: now)
    }
}

class PrivateBankFoxBondHoldingV2Converter : Converter<PrivateBankFoxBondHoldingEntity, PrivateBankFoxBondHoldingDto> {
    override fun toDto(entity: PrivateBankFoxBondHoldingEntity) = PrivateBankFoxBondHoldingDto(entity.id.toInt(), entity.bondCode, entity.bankCode, entity.principal, entity.rate, entity.startedAt, entity.dueAt, entity.redeemedAt)
    override fun toEntity(dto: PrivateBankFoxBondHoldingDto): PrivateBankFoxBondHoldingEntity {
        val now = System.currentTimeMillis()
        return PrivateBankFoxBondHoldingEntity(dto.id.toLong(), dto.bondCode, dto.bankCode, dto.principal, dto.rate, dto.startedAt.takeIf { it != 0L } ?: now, dto.dueAt.takeIf { it != 0L } ?: now, dto.redeemedAt)
    }
}
