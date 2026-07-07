package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.model.privatebank.PrivateBankDto

fun PrivateBankDto.isDefaulter(now: Long = System.currentTimeMillis()): Boolean =
    defaulterUntil != 0L && defaulterUntil > now
