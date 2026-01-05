package cn.chahuyun.economy.repository

import cn.chahuyun.economy.entity.UserRaffle
import cn.chahuyun.hibernateplus.HibernateFactory

/**
 * 抽奖统计持久化层（封装 HibernateFactory 访问）。
 */
object UserRaffleRepository {

    @JvmStatic
    fun findById(userId: Long): UserRaffle? = HibernateFactory.selectOneById(userId)

    @JvmStatic
    fun save(userRaffle: UserRaffle): UserRaffle = HibernateFactory.merge(userRaffle)
}


