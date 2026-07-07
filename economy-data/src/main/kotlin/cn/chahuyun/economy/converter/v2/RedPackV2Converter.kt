package cn.chahuyun.economy.converter.v2

import cn.chahuyun.economy.converter.Converter
import cn.chahuyun.economy.entity.v2.redpack.RedPackEntity
import cn.chahuyun.economy.model.redpack.RedPackDto
import cn.chahuyun.economy.model.redpack.RedPackKind

class RedPackV2Converter : Converter<RedPackEntity, RedPackDto> {

    override fun toDto(entity: RedPackEntity): RedPackDto {
        return RedPackDto(
            id = entity.id.toInt(),
            name = entity.name,
            groupId = entity.groupId,
            sender = entity.sender,
            money = entity.money,
            number = entity.number,
            createTime = entity.createTime,
            type = entity.type.toRedPackKind(),
            password = entity.password,
            takenMoneys = entity.takenMoneys,
            receiverList = entity.receivers.toLongList(),
            randomPackList = entity.randomRedPack.toDoubleList()
        )
    }

    override fun toEntity(dto: RedPackDto): RedPackEntity {
        val now = System.currentTimeMillis()
        return RedPackEntity(
            id = dto.id.toLong(),
            name = dto.name,
            groupId = dto.groupId,
            sender = dto.sender,
            money = dto.money,
            number = dto.number,
            createTime = dto.createTime,
            type = dto.type.name,
            password = dto.password,
            takenMoneys = dto.takenMoneys,
            receivers = dto.receiverList.joinToString(","),
            randomRedPack = dto.randomPackList.joinToString(",") { (Math.round(it * 10.0) / 10.0).toString() },
            createdAt = now,
            updatedAt = now
        )
    }

    private fun String.toRedPackKind(): RedPackKind {
        return runCatching { RedPackKind.valueOf(this) }.getOrDefault(RedPackKind.NORMAL)
    }

    private fun String.toLongList(): List<Long> {
        return split(",").mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toLongOrNull() }
    }

    private fun String.toDoubleList(): List<Double> {
        return split(",").mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toDoubleOrNull() }
    }
}
