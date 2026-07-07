package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBankFoxBondBid
import cn.chahuyun.economy.model.privatebank.PrivateBankFoxBondBidDto

/**
 * PrivateBankFoxBondBid V1实体与DTO转换器
 */
class PrivateBankFoxBondBidV1Converter : Converter<PrivateBankFoxBondBid, PrivateBankFoxBondBidDto> {

    override fun toDto(entity: PrivateBankFoxBondBid): PrivateBankFoxBondBidDto {
        return PrivateBankFoxBondBidDto(
            id = entity.id,
            bondCode = entity.bondCode,
            bankCode = entity.bankCode,
            ownerQq = entity.ownerQq,
            premium = entity.premium,
            bidRate = entity.bidRate,
            createdAt = entity.createdAt.time
        )
    }

    override fun toEntity(dto: PrivateBankFoxBondBidDto): PrivateBankFoxBondBid {
        return PrivateBankFoxBondBid().apply {
            id = dto.id
            bondCode = dto.bondCode
            bankCode = dto.bankCode
            ownerQq = dto.ownerQq
            premium = dto.premium
            bidRate = dto.bidRate
        }
    }
}
