package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.rob.RobInfo
import cn.chahuyun.economy.model.rob.RobInfoDto
import java.util.*

/**
 * RobInfo V1实体与DTO转换器
 */
class RobInfoV1Converter : Converter<RobInfo, RobInfoDto> {

    override fun toDto(entity: RobInfo): RobInfoDto {
        return RobInfoDto(
            userId = entity.userId ?: 0,
            nowTime = entity.nowTime?.time ?: 0,
            beRobNumber = entity.beRobNumber ?: 0,
            robSuccess = entity.robSuccess ?: 0,
            hitSuccess = entity.hitSuccess ?: 0
        )
    }

    override fun toEntity(dto: RobInfoDto): RobInfo {
        return RobInfo().apply {
            userId = dto.userId
            nowTime = dto.nowTime.takeIf { it > 0 }?.let { Date(it) }
            beRobNumber = dto.beRobNumber
            robSuccess = dto.robSuccess
            hitSuccess = dto.hitSuccess
        }
    }
}
