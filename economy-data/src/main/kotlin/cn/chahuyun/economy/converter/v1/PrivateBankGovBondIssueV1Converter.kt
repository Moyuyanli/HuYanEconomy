package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBankGovBondIssue
import cn.chahuyun.economy.model.privatebank.PrivateBankGovBondIssueDto

/**
 * PrivateBankGovBondIssue V1实体与DTO转换器
 */
class PrivateBankGovBondIssueV1Converter : Converter<PrivateBankGovBondIssue, PrivateBankGovBondIssueDto> {

    override fun toDto(entity: PrivateBankGovBondIssue): PrivateBankGovBondIssueDto {
        return PrivateBankGovBondIssueDto(
            id = entity.id,
            weekKey = entity.weekKey,
            rateMultiplier = entity.rateMultiplier,
            lockDays = entity.lockDays,
            totalLimit = entity.totalLimit,
            remaining = entity.remaining,
            createdAt = entity.createdAt.time
        )
    }

    override fun toEntity(dto: PrivateBankGovBondIssueDto): PrivateBankGovBondIssue {
        return PrivateBankGovBondIssue().apply {
            id = dto.id
            weekKey = dto.weekKey
            rateMultiplier = dto.rateMultiplier
            lockDays = dto.lockDays
            totalLimit = dto.totalLimit
            remaining = dto.remaining
        }
    }
}
