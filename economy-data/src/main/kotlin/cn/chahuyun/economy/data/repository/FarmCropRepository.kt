package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.entity.farm.FarmCrop

object FarmCropRepository {

    @JvmStatic
    fun listCrops(): List<FarmCrop> =
        HibernateDataStore.selectList(FarmCrop::class.java).sortedBy { it.level }

    @JvmStatic
    fun saveCrop(crop: FarmCrop): FarmCrop =
        HibernateDataStore.merge(crop)

    @JvmStatic
    fun saveCrops(crops: List<FarmCrop>): List<FarmCrop> =
        crops.map { saveCrop(it) }
}
