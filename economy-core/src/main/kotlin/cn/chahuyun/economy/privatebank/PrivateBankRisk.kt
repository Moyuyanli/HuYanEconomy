package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.model.privatebank.PrivateBankDto

fun PrivateBankDto.isBankrupt(): Boolean = bankruptAt > 0

fun PrivateBankDto.isDefaulter(now: Long = System.currentTimeMillis()): Boolean =
    isBankrupt() || (defaulterUntil != 0L && defaulterUntil > now) ||
        (code.isNotBlank() && runCatching { PrivateBankDebtService.hasOutstanding(code) }.getOrDefault(false))
