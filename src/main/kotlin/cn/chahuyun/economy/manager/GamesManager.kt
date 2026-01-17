package cn.chahuyun.economy.manager

import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.constant.FishPondLevelConstant
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.fish.FishInfo
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.plugin.FactorManager
import cn.chahuyun.economy.repository.FishRepository
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import cn.hutool.cron.CronUtil
import cn.hutool.cron.task.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * 游戏相关的“非事件监听”逻辑。
 *
 * 说明：
 * - `action.GamesAction` 仅保留 @MessageAuthorize 指令入口与自定义事件监听入口。
 * - 这里负责：定时任务 init/shutdown、钓鱼冷却/失败判定、以及若干可复用的业务函数。
 */
object GamesManager : CoroutineScope {

    private val supervisorJob = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + supervisorJob

    private val playerCooling = ConcurrentHashMap<Long, Date>()
    private val isProcessing = ConcurrentHashMap<Long, AtomicBoolean>()

    @JvmStatic
    fun init() {
        val task = Task {
            launch {
                val groupPonds = FishRepository.listGroupPonds()
                val levels = FishPondLevelConstant.entries.toTypedArray()

                for (fishPond in groupPonds) {
                    try {
                        val currentLevelIndex = fishPond.pondLevel - 1

                        // 如果已经是最高等级，跳过
                        if (currentLevelIndex >= levels.size) {
                            continue
                        }

                        val nextLevelConfig = levels[currentLevelIndex]
                        val upgradeCost = nextLevelConfig.amount.toDouble()

                        if (fishPond.getFishPondMoney() >= upgradeCost) {
                            if (
                                EconomyUtil.plusMoneyToPluginBankForId(
                                    fishPond.code,
                                    fishPond.description ?: "",
                                    -upgradeCost
                                )
                            ) {
                                val oldLevel = fishPond.pondLevel
                                val newLevel = oldLevel + 1

                                fishPond.pondLevel = newLevel
                                fishPond.minLevel = nextLevelConfig.minFishLevel
                                val saved = runCatching { fishPond.save() }.getOrNull()
                                if (saved == null) {
                                    // 回滚：升级状态未落库时把扣除的鱼塘资金退回
                                    EconomyUtil.plusMoneyToPluginBankForId(
                                        fishPond.code,
                                        fishPond.description ?: "",
                                        upgradeCost
                                    )
                                    throw IllegalStateException("鱼塘升级落库失败，已退款")
                                }

                                val bot = if (Bot.instances.isNotEmpty()) Bot.instances[0] else null
                                if (bot != null) {
                                    val group = bot.getGroup(fishPond.group)
                                    if (group != null) {
                                        group.sendMessage(
                                            "鱼塘 [${fishPond.name}] 已经积攒够了升级的资金！开始升级鱼塘了！\n" +
                                                    "鱼塘等级: $oldLevel -> $newLevel\n" +
                                                    "最低鱼竿等级限制: ${nextLevelConfig.minFishLevel}"
                                        )
                                    } else {
                                        bot.getFriend(fishPond.admin)
                                            ?.sendMessage("群鱼塘 ${fishPond.name} 升级到 $newLevel 级了")
                                    }
                                }
                                Log.info("游戏管理: 鱼塘 ${fishPond.name} 升级成功: $oldLevel -> $newLevel")
                            }
                        }
                    } catch (e: Exception) {
                        Log.error("游戏管理: 鱼塘 ${fishPond.name}(${fishPond.code}) 自动升级异常", e)
                    }
                }
            }
        }
        CronUtil.schedule("0 0 12 * * ?", task)
    }

    @JvmStatic
    fun shutdown() {
        Log.info("游戏管理:正在关闭游戏线程...")
        supervisorJob.cancel()
    }

    @JvmStatic
    fun clearCooling() {
        playerCooling.clear()
    }

    @JvmStatic
    fun removeCooling(qq: Long) {
        playerCooling.remove(qq)
    }

    @JvmStatic
    suspend fun checkAndProcessFishing(
        userInfo: UserInfo,
        isFishingTitle: Boolean,
        fishInfo: FishInfo,
        subject: Contact,
        chain: net.mamoe.mirai.message.data.MessageChain,
    ): Boolean {
        val qq = userInfo.qq
        if (isProcessing.putIfAbsent(qq, AtomicBoolean(true)) != null) {
            subject.sendMessage(MessageUtil.formatMessageChain(chain, "请稍后再试!"))
            return true
        }

        return try {
            playerCooling[qq]?.let { lastDate ->
                val between = DateUtil.between(lastDate, Date(), DateUnit.MINUTE, true)
                var expired = if (isFishingTitle) 5 else (10 * 60 - fishInfo.rodLevel * 3) / 60

                FactorManager.getUserFactor(userInfo).getBuffValue(FunctionProps.RED_EYES)?.let { buff ->
                    if (DateUtil.between(DateUtil.parse(buff), Date(), DateUnit.MINUTE) <= 60) {
                        expired -= (expired * 0.8).toInt()
                    } else {
                        FactorManager.merge(
                            FactorManager.getUserFactor(userInfo).apply { setBuffValue(FunctionProps.RED_EYES, null) })
                    }
                }

                if (between < expired) {
                    subject.sendMessage(
                        MessageUtil.formatMessageChain(
                            chain,
                            "你还差${expired - between}分钟来抛第二杆!"
                        )
                    )
                    return true
                }
            }
            playerCooling[qq] = Date()
            false
        } finally {
            isProcessing.remove(qq)
        }
    }

    @JvmStatic
    suspend fun failedFishing(userInfo: UserInfo, user: User, subject: Contact, fishInfo: FishInfo): Boolean {
        val errorMessages = arrayOf("风吹的...", "眼花了...", "走神了...", "呀！切线了...", "钓鱼佬绝不空军！")
        val randomed = RandomUtil.randomInt(0, 10001)
        return when {
            randomed >= 9998 -> {
                subject.sendMessage(
                    MessageUtil.formatMessageChain(
                        user.id,
                        "你钓起来一具尸体，附近的钓鱼佬报警了，你真是百口模辩啊！"
                    )
                )
                UserStatusManager.movePrison(userInfo, 60)
                fishInfo.switchStatus()
                true
            }

            randomed >= 6000 -> {
                subject.sendMessage(MessageUtil.formatMessageChain(user.id, errorMessages[RandomUtil.randomInt(0, 5)]))
                fishInfo.switchStatus()
                true
            }

            else -> false
        }
    }

    /**
     * 下列函数目前未通过 @MessageAuthorize 直接暴露，保留为可复用的业务接口。
     */
    @JvmStatic
    suspend fun buyFishRod(event: net.mamoe.mirai.event.events.MessageEvent) {
        Log.info("购买鱼竿指令")
        val userInfo = UserCoreManager.getUserInfo(event.sender)
        val fishInfo = userInfo.getFishInfo()
        val subject = event.subject

        if (fishInfo.isFishRod) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    HuYanEconomy.msgConfig?.repeatPurchaseRod ?: ""
                )
            )
            return
        }

        val moneyByUser = EconomyUtil.getMoneyByUser(event.sender)
        if (moneyByUser < 500) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    HuYanEconomy.msgConfig?.coinNotEnoughForRod ?: ""
                )
            )
            return
        }

        if (EconomyUtil.minusMoneyToUser(event.sender, 500.0)) {
            fishInfo.isFishRod = true
            FishRepository.saveFishInfo(fishInfo)
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    HuYanEconomy.msgConfig?.buyFishingRodSuccess ?: ""
                )
            )
        } else {
            Log.error("游戏管理:购买鱼竿失败!")
        }
    }

    @JvmStatic
    suspend fun upFishRod(event: net.mamoe.mirai.event.events.MessageEvent) {
        Log.info("升级鱼竿指令")
        val userInfo = UserCoreManager.getUserInfo(event.sender)
        val subject = event.subject
        val fishInfo = userInfo.getFishInfo()

        if (!fishInfo.isFishRod) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    HuYanEconomy.msgConfig?.noneRodUpgradeMsg ?: ""
                )
            )
            return
        }
        if (fishInfo.status) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(
                    event.message,
                    HuYanEconomy.msgConfig?.upgradeWhenFishing ?: ""
                )
            )
            return
        }
        subject.sendMessage(fishInfo.updateRod(userInfo))
    }

    @JvmStatic
    suspend fun fishTop(event: net.mamoe.mirai.event.events.MessageEvent) {
        Log.info("钓鱼榜指令")
        val bot = event.bot
        val subject = event.subject

        val rankingList = FishRepository.topRankingByMoney(limit = 10)

        if (rankingList.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.message, "暂时没人钓鱼!"))
            return
        }

        val forwardMessage = ForwardMessageBuilder(subject).apply {
            add(bot, PlainText("钓鱼排行榜:"))
            rankingList.forEachIndexed { index, ranking ->
                add(bot, ranking.getInfo(index))
            }
        }
        subject.sendMessage(forwardMessage.build())
    }

    @JvmStatic
    suspend fun viewFishLevel(event: net.mamoe.mirai.event.events.MessageEvent) {
        Log.info("鱼竿等级指令")
        val userInfo = UserCoreManager.getUserInfo(event.sender)
        val rodLevel = userInfo.getFishInfo().rodLevel
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你的鱼竿等级为${rodLevel}级"))
    }
}


