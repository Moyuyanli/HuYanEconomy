package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.privatebank.PrivateBankEntity
import cn.chahuyun.economy.model.privatebank.PrivateBankDto

class PrivateBankV2Converter : Converter<PrivateBankEntity, PrivateBankDto> {

    override fun toDto(entity: PrivateBankEntity): PrivateBankDto {
        return PrivateBankDto(
            id = entity.id.toInt(),
            code = entity.code,
            name = entity.name,
            slogan = entity.slogan,
            ownerQq = entity.ownerQq,
            vipOnly = entity.vipOnly,
            vipWhitelist = entity.vipWhitelist,
            depositorInterest = entity.depositorInterest,
            createdAt = entity.createdAt,
            defaulterUntil = entity.defaulterUntil,
            withdrawRequests = entity.withdrawRequests,
            withdrawFailures = entity.withdrawFailures,
            star = entity.star,
            avgReview = entity.avgReview
        )
    }

    override fun toEntity(dto: PrivateBankDto): PrivateBankEntity {
        val now = System.currentTimeMillis()
        return PrivateBankEntity(
            id = dto.id.toLong(),
            code = dto.code,
            name = dto.name,
            slogan = dto.slogan,
            ownerQq = dto.ownerQq,
            vipOnly = dto.vipOnly,
            vipWhitelist = dto.vipWhitelist,
            depositorInterest = dto.depositorInterest,
            createdAt = dto.createdAt.takeIf { it != 0L } ?: now,
            defaulterUntil = dto.defaulterUntil,
            withdrawRequests = dto.withdrawRequests,
            withdrawFailures = dto.withdrawFailures,
            star = dto.star,
            avgReview = dto.avgReview,
            updatedAt = now
        )
    }
}
