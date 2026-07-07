package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.bank.BankEntity
import cn.chahuyun.economy.model.bank.BankInfoDto

/**
 * Bank V2 entity and DTO converter.
 */
class BankInfoV2Converter : Converter<BankEntity, BankInfoDto> {

    override fun toDto(entity: BankEntity): BankInfoDto {
        return BankInfoDto(
            id = entity.id.toInt(),
            code = entity.code,
            name = entity.name,
            description = entity.description,
            qq = entity.qq,
            interestSwitch = entity.interestSwitch,
            regTime = entity.regTime,
            regTotal = entity.regTotal,
            total = entity.total,
            interest = entity.interest
        )
    }

    override fun toEntity(dto: BankInfoDto): BankEntity {
        val now = System.currentTimeMillis()
        return BankEntity(
            id = dto.id.toLong(),
            code = dto.code,
            name = dto.name,
            description = dto.description,
            qq = dto.qq,
            interestSwitch = dto.interestSwitch,
            regTime = dto.regTime,
            regTotal = dto.regTotal,
            total = dto.total,
            interest = dto.interest,
            createdAt = now,
            updatedAt = now
        )
    }
}
