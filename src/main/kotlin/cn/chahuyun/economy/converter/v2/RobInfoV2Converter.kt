package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.rob.RobInfoEntity
import cn.chahuyun.economy.model.rob.RobInfoDto

class RobInfoV2Converter : Converter<RobInfoEntity, RobInfoDto> {

    override fun toDto(entity: RobInfoEntity): RobInfoDto {
        return RobInfoDto(
            userId = entity.userId,
            nowTime = entity.nowTime,
            beRobNumber = entity.beRobNumber,
            robSuccess = entity.robSuccess,
            hitSuccess = entity.hitSuccess
        )
    }

    override fun toEntity(dto: RobInfoDto): RobInfoEntity {
        val now = System.currentTimeMillis()
        return RobInfoEntity(
            userId = dto.userId,
            nowTime = dto.nowTime,
            beRobNumber = dto.beRobNumber,
            robSuccess = dto.robSuccess,
            hitSuccess = dto.hitSuccess,
            createdAt = now,
            updatedAt = now
        )
    }
}
