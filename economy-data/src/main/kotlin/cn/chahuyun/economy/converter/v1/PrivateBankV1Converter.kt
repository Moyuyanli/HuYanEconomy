package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBank
import cn.chahuyun.economy.model.privatebank.PrivateBankDto
import java.util.*

/**
 * PrivateBank V1实体与DTO转换器
 */
class PrivateBankV1Converter : Converter<PrivateBank, PrivateBankDto> {

    override fun toDto(entity: PrivateBank): PrivateBankDto {
        return PrivateBankDto(
            id = entity.id,
            code = entity.code,
            name = entity.name,
            slogan = entity.slogan ?: "",
            ownerQq = entity.ownerQq,
            vipOnly = entity.vipOnly,
            vipWhitelist = entity.vipWhitelist ?: "",
            depositorInterest = entity.depositorInterest,
            createdAt = entity.createdAt.time,
            defaulterUntil = entity.defaulterUntil?.time ?: 0,
            withdrawRequests = entity.withdrawRequests,
            withdrawFailures = entity.withdrawFailures,
            star = entity.star,
            avgReview = entity.avgReview
        )
    }

    override fun toEntity(dto: PrivateBankDto): PrivateBank {
        return PrivateBank().apply {
            id = dto.id
            code = dto.code
            name = dto.name
            slogan = dto.slogan.ifEmpty { null }
            ownerQq = dto.ownerQq
            vipOnly = dto.vipOnly
            vipWhitelist = dto.vipWhitelist.ifEmpty { null }
            depositorInterest = dto.depositorInterest
            createdAt = Date(dto.createdAt.takeIf { it != 0L } ?: Date().time)
            defaulterUntil = dto.defaulterUntil.takeIf { it != 0L }?.let(::Date)
            withdrawRequests = dto.withdrawRequests
            withdrawFailures = dto.withdrawFailures
            star = dto.star
            avgReview = dto.avgReview
        }
    }
}
