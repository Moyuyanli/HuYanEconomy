package cn.chahuyun.economy.games.farm

import cn.chahuyun.economy.data.repository.FarmCropRepository
import cn.chahuyun.economy.entity.v1.farm.FarmCrop

object FarmCropStorageService {

    fun listCrops(): List<FarmCrop> =
        FarmCropRepository.listCrops()

    fun saveCrops(crops: List<FarmCrop>): List<FarmCrop> =
        FarmCropRepository.saveCrops(crops)
}

