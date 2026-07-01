package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.privatebank.PrivateBankReview
import cn.chahuyun.economy.model.privatebank.PrivateBankReviewDto

/**
 * PrivateBankReview V1实体与DTO转换器
 */
class PrivateBankReviewV1Converter : Converter<PrivateBankReview, PrivateBankReviewDto> {

    override fun toDto(entity: PrivateBankReview): PrivateBankReviewDto {
        return PrivateBankReviewDto(
            id = entity.id,
            bankCode = entity.bankCode,
            userQq = entity.userQq,
            rating = entity.rating,
            content = entity.content ?: "",
            createdAt = entity.createdAt.time
        )
    }

    override fun toEntity(dto: PrivateBankReviewDto): PrivateBankReview {
        return PrivateBankReview().apply {
            id = dto.id
            bankCode = dto.bankCode
            userQq = dto.userQq
            rating = dto.rating
            content = dto.content.ifEmpty { null }
        }
    }
}
