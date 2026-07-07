package cn.chahuyun.economy.converter.v1

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.redpack.RedPack
import cn.chahuyun.economy.entity.redpack.RedPackType
import cn.chahuyun.economy.model.redpack.RedPackDto
import cn.chahuyun.economy.model.redpack.RedPackKind
import java.util.*

/**
 * RedPack V1实体与DTO转换器
 */
class RedPackV1Converter : Converter<RedPack, RedPackDto> {

    override fun toDto(entity: RedPack): RedPackDto {
        return RedPackDto(
            id = entity.id ?: 0,
            name = entity.name ?: "",
            groupId = entity.groupId ?: 0,
            sender = entity.sender ?: 0,
            money = entity.money ?: 0.0,
            number = entity.number ?: 0,
            createTime = entity.createTime?.time ?: 0,
            type = entity.type.toDtoKind(),
            password = entity.password ?: "",
            takenMoneys = entity.takenMoneys,
            receiverList = entity.receiverList.toList(),
            randomPackList = entity.randomPackList.toList()
        )
    }

    override fun toEntity(dto: RedPackDto): RedPack {
        return RedPack().apply {
            if (dto.id != 0) id = dto.id
            name = dto.name.ifEmpty { null }
            groupId = dto.groupId
            sender = dto.sender
            money = dto.money
            number = dto.number
            createTime = dto.createTime.takeIf { it > 0 }?.let { Date(it) }
            type = dto.type.toEntityType()
            password = dto.password.ifEmpty { null }
            takenMoneys = dto.takenMoneys
            receiverList = dto.receiverList.toMutableList()
            randomPackList = dto.randomPackList.toMutableList()
        }
    }

    private fun RedPackType.toDtoKind(): RedPackKind {
        return when (this) {
            RedPackType.NORMAL -> RedPackKind.NORMAL
            RedPackType.RANDOM -> RedPackKind.RANDOM
            RedPackType.PASSWORD -> RedPackKind.PASSWORD
        }
    }

    private fun RedPackKind.toEntityType(): RedPackType {
        return when (this) {
            RedPackKind.NORMAL -> RedPackType.NORMAL
            RedPackKind.RANDOM -> RedPackType.RANDOM
            RedPackKind.PASSWORD -> RedPackType.PASSWORD
        }
    }
}
