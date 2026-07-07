package cn.chahuyun.economy.service

import cn.chahuyun.economy.utils.ShareUtils
import cn.hutool.core.util.RandomUtil

object RedPackAllocationService {

    fun averagePack(totalAmount: Double, count: Int): Double = ShareUtils.rounding(totalAmount / count)

    fun generateRandomPack(totalAmount: Double, count: Int): List<Double> {
        val result = mutableListOf<Double>()
        var remainingAmount = totalAmount
        var remainingCount = count

        for (i in 0 until count - 1) {
            val avg = remainingAmount / remainingCount
            val max = avg * 2

            var amount = RandomUtil.randomDouble(0.1, max)
            amount = ShareUtils.rounding(amount)

            val minReserved = (remainingCount - 1) * 0.1
            if (remainingAmount - amount < minReserved) {
                amount = ShareUtils.rounding(remainingAmount - minReserved)
            }

            if (amount < 0.1) amount = 0.1

            result.add(amount)
            remainingAmount -= amount
            remainingCount--
        }
        result.add(ShareUtils.rounding(remainingAmount))
        return result
    }

    fun drawRandomPack(remainingPacks: MutableList<Double>): Double {
        if (remainingPacks.isEmpty()) {
            throw RuntimeException("红包已经被领取干净，但仍在领取流程中!")
        }
        val index = RandomUtil.randomInt(0, remainingPacks.size)
        return remainingPacks.removeAt(index)
    }
}
