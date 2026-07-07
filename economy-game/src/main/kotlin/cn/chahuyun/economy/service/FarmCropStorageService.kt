package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.repository.FarmCropRepository
import cn.chahuyun.economy.entity.farm.FarmCrop

object FarmCropStorageService {

    fun listCrops(): List<FarmCrop> =
        FarmCropRepository.listCrops()

    fun saveCrops(crops: List<FarmCrop>): List<FarmCrop> =
        FarmCropRepository.saveCrops(crops)
}
