package cn.chahuyun.economy.feature.redpack

import cn.chahuyun.economy.model.redpack.RedPackDto
import cn.chahuyun.economy.model.redpack.RedPackKind

val RedPackDto.isRandomAllocation: Boolean
    get() = type == RedPackKind.RANDOM || type == RedPackKind.PASSWORD
