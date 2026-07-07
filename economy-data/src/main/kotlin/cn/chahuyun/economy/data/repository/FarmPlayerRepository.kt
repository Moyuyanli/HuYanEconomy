package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.farm.FarmPlayer

object FarmPlayerRepository {

    @JvmStatic
    fun findPlayer(qq: Long): FarmPlayer? =
        HibernateDataStore.selectOne(FarmPlayer::class.java, "qq", qq)

    @JvmStatic
    fun savePlayer(player: FarmPlayer): FarmPlayer =
        HibernateDataStore.merge(player)
}
