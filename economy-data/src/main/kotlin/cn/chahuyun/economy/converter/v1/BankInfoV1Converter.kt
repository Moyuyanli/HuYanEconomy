package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.bank.BankInfo
import cn.chahuyun.economy.model.bank.BankInfoDto
import java.util.*

/**
 * BankInfo V1实体与DTO转换器
 */
class BankInfoV1Converter : Converter<BankInfo, BankInfoDto> {

    override fun toDto(entity: BankInfo): BankInfoDto {
        return BankInfoDto(
            id = entity.id,
            code = entity.code ?: "",
            name = entity.name ?: "",
            description = entity.description ?: "",
            qq = entity.qq,
            interestSwitch = entity.interestSwitch,
            regTime = entity.regTime?.time ?: 0,
            regTotal = entity.regTotal,
            total = entity.total,
            interest = entity.interest
        )
    }

    override fun toEntity(dto: BankInfoDto): BankInfo {
        return BankInfo().apply {
            id = dto.id
            code = dto.code.ifEmpty { null }
            name = dto.name.ifEmpty { null }
            description = dto.description.ifEmpty { null }
            qq = dto.qq
            interestSwitch = dto.interestSwitch
            regTime = dto.regTime.takeIf { it > 0 }?.let { Date(it) }
            regTotal = dto.regTotal
            total = dto.total
            interest = dto.interest
        }
    }
}
