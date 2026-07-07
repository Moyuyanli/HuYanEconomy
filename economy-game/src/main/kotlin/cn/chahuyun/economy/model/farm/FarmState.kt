package cn.chahuyun.economy.model.farm

import cn.chahuyun.economy.entity.farm.FarmInventory
import cn.chahuyun.economy.entity.farm.FarmPlayer
import cn.chahuyun.economy.entity.farm.FarmPlot

data class FarmState(
    val player: FarmPlayer,
    val plots: List<FarmPlot>,
    val inventory: List<FarmInventory>,
)
