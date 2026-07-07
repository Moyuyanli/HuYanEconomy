package cn.chahuyun.economy.service

import cn.chahuyun.economy.constant.FishPondLevelConstant
import cn.chahuyun.economy.data.repository.FishRepository
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.utils.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import kotlin.coroutines.CoroutineContext

object FishPondUpgradeService : CoroutineScope {
    private const val TASK_ID = "fish-pond-upgrade"
    private val supervisorJob = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + supervisorJob

    fun init() {
        HuYanScheduler.schedule(TASK_ID, "0 0 12 * * ?", Runnable {
            launch {
                upgradeEligiblePonds()
            }
        })
    }

    fun shutdown() {
        HuYanScheduler.cancel(TASK_ID)
        supervisorJob.cancel()
    }

    private suspend fun upgradeEligiblePonds() {
        val groupPonds = FishRepository.listGroupPonds()
        val levels = FishPondLevelConstant.entries.toTypedArray()

        for (fishPond in groupPonds) {
            try {
                if (fishPond.pondLevel >= FishPondLevelConstant.MAX_LEVEL) {
                    continue
                }

                val nextLevelConfig = levels[fishPond.pondLevel - 1]
                val upgradeCost = nextLevelConfig.amount.toDouble()
                if (EconomyAccountService.pluginBankBalance(fishPond.code, fishPond.description ?: "") < upgradeCost) {
                    continue
                }
                if (!EconomyAccountService.addPluginBank(fishPond.code, fishPond.description ?: "", -upgradeCost)) {
                    continue
                }

                val oldLevel = fishPond.pondLevel
                val newLevel = oldLevel + 1
                fishPond.pondLevel = newLevel
                fishPond.minLevel = nextLevelConfig.minFishLevel

                val saved = runCatching { FishRepository.saveFishPond(fishPond) }.getOrNull()
                if (saved == null) {
                    EconomyAccountService.addPluginBank(fishPond.code, fishPond.description ?: "", upgradeCost)
                    throw IllegalStateException("鱼塘升级落库失败，已退款")
                }

                notifyUpgrade(
                    fishPond.groupId,
                    fishPond.admin,
                    fishPond.name ?: "",
                    oldLevel,
                    newLevel,
                    nextLevelConfig.minFishLevel
                )
                Log.info("游戏管理: 鱼塘 ${fishPond.name} 升级成功: $oldLevel -> $newLevel")
            } catch (e: Exception) {
                Log.error("游戏管理: 鱼塘 ${fishPond.name}(${fishPond.code}) 自动升级异常", e)
            }
        }
    }

    private suspend fun notifyUpgrade(
        groupId: Long,
        adminId: Long,
        pondName: String,
        oldLevel: Int,
        newLevel: Int,
        minFishLevel: Int,
    ) {
        val bot = if (Bot.instances.isNotEmpty()) Bot.instances[0] else return
        val group = bot.getGroup(groupId)
        if (group != null) {
            group.sendMessage(
                "鱼塘 [$pondName] 已经积攒够了升级的资金！开始升级鱼塘了！\n" +
                    "鱼塘等级: $oldLevel -> $newLevel\n" +
                    "最低鱼竿等级限制: $minFishLevel"
            )
        } else {
            bot.getFriend(adminId)?.sendMessage("群鱼塘 $pondName 升级到 $newLevel 级了")
        }
    }
}
