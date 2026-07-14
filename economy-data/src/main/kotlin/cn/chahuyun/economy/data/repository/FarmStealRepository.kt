package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.entity.farm.FarmInventory
import cn.chahuyun.economy.entity.farm.FarmPlot

object FarmStealRepository {

    /**
     * Atomically marks a crop season as stolen and credits the thief's farm inventory.
     */
    @JvmStatic
    fun recordSuccessfulSteal(
        plotId: Long,
        expectedSeason: Int,
        expectedCropCode: String,
        thiefQq: Long,
        fruitItemType: String,
        amount: Int,
    ): Boolean = HibernateDataStore.getSessionFactory().fromTransaction { session ->
        val plot = session.find(FarmPlot::class.java, plotId) ?: return@fromTransaction false
        if (
            plot.status != FarmConstants.PLOT_PLANTED ||
            plot.currentSeason != expectedSeason ||
            plot.cropCode != expectedCropCode ||
            plot.stolenSeason == expectedSeason
        ) {
            return@fromTransaction false
        }

        val inventory = session.createQuery(
            "from FarmInventory where qq = :qq and itemType = :itemType and itemCode = :itemCode",
            FarmInventory::class.java,
        )
            .setParameter("qq", thiefQq)
            .setParameter("itemType", fruitItemType)
            .setParameter("itemCode", expectedCropCode)
            .setMaxResults(1)
            .resultList
            .firstOrNull()
            ?: FarmInventory().apply {
                qq = thiefQq
                itemType = fruitItemType
                itemCode = expectedCropCode
                session.persist(this)
            }

        inventory.amount += amount
        plot.stolenSeason = expectedSeason
        plot.stolenAmount = amount
        true
    }
}
