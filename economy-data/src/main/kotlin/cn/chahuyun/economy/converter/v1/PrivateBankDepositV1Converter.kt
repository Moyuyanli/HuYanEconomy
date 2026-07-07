package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBankDeposit
import cn.chahuyun.economy.model.privatebank.PrivateBankDepositDto
import java.util.*

/**
 * PrivateBankDeposit V1实体与DTO转换器
 */
class PrivateBankDepositV1Converter : Converter<PrivateBankDeposit, PrivateBankDepositDto> {

    override fun toDto(entity: PrivateBankDeposit): PrivateBankDepositDto {
        return PrivateBankDepositDto(
            id = entity.id,
            bankCode = entity.bankCode,
            userQq = entity.userQq,
            principal = entity.principal,
            createdAt = entity.createdAt.time,
            updatedAt = entity.updatedAt.time
        )
    }

    override fun toEntity(dto: PrivateBankDepositDto): PrivateBankDeposit {
        return PrivateBankDeposit().apply {
            id = dto.id
            bankCode = dto.bankCode
            userQq = dto.userQq
            principal = dto.principal
            createdAt = Date(dto.createdAt.takeIf { it != 0L } ?: Date().time)
            updatedAt = Date(dto.updatedAt.takeIf { it != 0L } ?: Date().time)
        }
    }
}
