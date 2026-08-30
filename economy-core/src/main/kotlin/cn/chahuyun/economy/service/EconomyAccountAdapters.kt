package cn.chahuyun.economy.service

import cn.chahuyun.economy.base.EconomyAccount
import cn.chahuyun.economy.model.user.UserInfoDto

/** Converts the existing data DTO to the new local economy account handle. */
fun UserInfoDto.economyAccount(): EconomyAccount = EconomyAccount.take("u$qq")
