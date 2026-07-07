package cn.chahuyun.economy.service

import cn.chahuyun.economy.entity.fish.FishPond
import cn.chahuyun.economy.model.fish.FishPondDto

private val groupCodeRegex = Regex("g-(\\d+)")

val FishPond.groupId: Long
    get() = parseFishPondGroupId(code)

val FishPondDto.groupId: Long
    get() = parseFishPondGroupId(code)

fun parseFishPondGroupId(code: String): Long =
    groupCodeRegex.find(code)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
