package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.repository.FarmPlayerRepository
import cn.chahuyun.economy.entity.farm.FarmPlayer

object FarmPlayerService {

    fun findOrCreatePlayer(qq: Long): FarmPlayer =
        FarmPlayerRepository.findPlayer(qq) ?: savePlayer(FarmPlayer().apply { this.qq = qq })

    fun savePlayer(player: FarmPlayer): FarmPlayer =
        FarmPlayerRepository.savePlayer(player)
}
