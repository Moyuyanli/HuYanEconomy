package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBankFoxBond
import cn.chahuyun.economy.model.privatebank.PrivateBankFoxBondDto
import java.util.*

/**
 * PrivateBankFoxBond V1实体与DTO转换器
 */
class PrivateBankFoxBondV1Converter : Converter<PrivateBankFoxBond, PrivateBankFoxBondDto> {

    override fun toDto(entity: PrivateBankFoxBond): PrivateBankFoxBondDto {
        return PrivateBankFoxBondDto(
            id = entity.id,
            code = entity.code,
            faceValue = entity.faceValue,
            baseRate = entity.baseRate,
            termDays = entity.termDays,
            bidStartAt = entity.bidStartAt.time,
            bidEndAt = entity.bidEndAt.time,
            status = entity.status,
            winnerBankCode = entity.winnerBankCode ?: "",
            winnerBidRate = entity.winnerBidRate ?: 0.0,
            winnerPremium = entity.winnerPremium ?: 0.0,
            createdAt = entity.createdAt.time
        )
    }

    override fun toEntity(dto: PrivateBankFoxBondDto): PrivateBankFoxBond {
        return PrivateBankFoxBond().apply {
            id = dto.id
            code = dto.code
            faceValue = dto.faceValue
            baseRate = dto.baseRate
            termDays = dto.termDays
            bidStartAt = Date(dto.bidStartAt.takeIf { it != 0L } ?: Date().time)
            bidEndAt = Date(dto.bidEndAt.takeIf { it != 0L } ?: Date().time)
            status = dto.status
            winnerBankCode = dto.winnerBankCode.ifEmpty { null }
            winnerBidRate = dto.winnerBidRate
            winnerPremium = dto.winnerPremium
            createdAt = Date(dto.createdAt.takeIf { it != 0L } ?: Date().time)
        }
    }
}
