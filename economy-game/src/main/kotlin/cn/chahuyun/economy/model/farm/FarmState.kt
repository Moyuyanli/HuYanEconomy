package cn.chahuyun.economy.model.farm

import cn.chahuyun.economy.entity.v1.farm.FarmInventory
import cn.chahuyun.economy.entity.v1.farm.FarmPlayer
import cn.chahuyun.economy.entity.v1.farm.FarmPlot

data class FarmState(
    val player: FarmPlayer,
    val plots: List<FarmPlot>,
    val inventory: List<FarmInventory>,
)

