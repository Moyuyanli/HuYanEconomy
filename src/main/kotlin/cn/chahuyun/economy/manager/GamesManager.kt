package cn.chahuyun.economy.manager

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.constant.FishPondLevelConstant
import cn.chahuyun.economy.constant.PropsKind
import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.entity.UserInfo
import cn.chahuyun.economy.entity.fish.*
import cn.chahuyun.economy.fish.FishRollEvent
import cn.chahuyun.economy.fish.FishStartEvent
import cn.chahuyun.economy.manager.BackpackManager
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.manager.UserManager
import cn.chahuyun.economy.manager.UserStatusManager
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.plugin.FactorManager
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.prop.Stackable
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import cn.hutool.cron.CronUtil
import cn.hutool.cron.task.Task
import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext

/**
 * 游戏管理
 * 钓鱼重构为 Kotlin
 *
 * @author Moyuyanli
 * @date 2025/12/22
 */
@EventComponent
class GamesManager : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + supervisorJob

    companion object : CoroutineScope {
        private val supervisorJob = SupervisorJob()
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Default + supervisorJob

        private val playerCooling = ConcurrentHashMap<Long, Date>()
        private val isProcessing = ConcurrentHashMap<Long, AtomicBoolean>()

        @JvmStatic
        fun init() {
            val task = Task {
                launch {
                    val groupPonds = HibernateFactory.selectList(FishPond::class.java, "pondType", 1)
                    val levels = FishPondLevelConstant.values()

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
                                if (EconomyUtil.plusMoneyToPluginBankForId(
                                        fishPond.code,
                                        fishPond.description ?: "",
                                        -upgradeCost
                                    )
                                ) {
                                    val oldLevel = fishPond.pondLevel
                                    val newLevel = oldLevel + 1

                                    fishPond.pondLevel = newLevel
                                    fishPond.minLevel = nextLevelConfig.minFishLevel
                                    fishPond.save()

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
        fun fishStart(event: FishStartEvent) {
            val userInfo = event.userInfo
            val toRemove = mutableListOf<Long>()

            for (backpack in userInfo.backpacks) {
                if (event.fishBait == null && backpack.propKind == PropsKind.fishBait) {
                    val bait = try {
                        PropsManager.getProp(backpack, cn.chahuyun.economy.model.fish.FishBait::class.java)
                    } catch (e: Exception) {
                        if (e.message == "该道具数据不存在") {
                            toRemove.add(backpack.propId ?: 0L)
                            continue
                        } else throw e
                    }

                    when {
                        bait.num > 1 -> {
                            PropsManager.useProp(backpack, UseEvent(null, null, userInfo))
                            event.fishBait = bait
                        }

                        bait.num == 1 -> {
                            toRemove.add(backpack.propId ?: 0L)
                            event.fishBait = PropsManager.copyProp(bait)
                        }

                        else -> {
                            event.fishBait = cn.chahuyun.economy.model.fish.FishBait().apply {
                                level = 1
                                quality = 0.01f
                                name = "空钩"
                            }
                            BackpackManager.delPropToBackpack(userInfo, backpack.propId ?: 0L)
                        }
                    }
                }
            }

            toRemove.forEach { BackpackManager.delPropToBackpack(userInfo, it) }

            event.fishBait?.let {
                event.maxDifficulty = event.calculateMaxDifficulty()
                event.minDifficulty = event.calculateMinDifficulty()
                event.maxGrade = event.calculateMaxGrade()
            }
        }

        @JvmStatic
        fun fishRoll(event: FishRollEvent) {
            val minDifficulty = (event.minDifficulty ?: 1).coerceAtLeast(1)
            val maxDifficulty = event.maxDifficulty ?: 1
            val minGrade = (event.minGrade ?: 1).coerceAtLeast(1)
            var maxGrade = event.maxGrade ?: 1
            val fishPond = event.fishPond

            var rank = RandomUtil.randomInt(minGrade, (maxGrade + 1).coerceAtLeast(minGrade + 1))

            while (true) {
                val difficulty =
                    RandomUtil.randomInt(minDifficulty, (maxDifficulty + 1).coerceAtLeast(minDifficulty + 1))
                val levelFishList = fishPond.getFishList(rank)
                val collect = levelFishList.filter { it.difficulty <= difficulty }
                    .sortedBy { it.description }

                if (collect.isEmpty()) {
                    if (rank > 1) {
                        rank--
                        continue
                    } else {
                        event.fish = levelFishList.firstOrNull()
                        break
                    }
                }
                event.fish = collect[RandomUtil.randomInt(0, collect.size.coerceAtMost(4))]
                break
            }
        }

        @JvmStatic
        suspend fun buyFishRod(event: MessageEvent) {
            Log.info("购买鱼竿指令")
            val userInfo = UserManager.getUserInfo(event.sender)
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
                HibernateFactory.merge(fishInfo)
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
        suspend fun upFishRod(event: MessageEvent) {
            Log.info("升级鱼竿指令")
            val userInfo = UserManager.getUserInfo(event.sender)
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
        suspend fun fishTop(event: MessageEvent) {
            Log.info("钓鱼榜指令")
            val bot = event.bot
            val subject = event.subject

            val rankingList = HibernateFactory.selectList(FishRanking::class.java)
                .sortedByDescending { it.money }
                .take(10)

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
        suspend fun viewFishLevel(event: MessageEvent) {
            Log.info("鱼竿等级指令")
            val userInfo = UserManager.getUserInfo(event.sender)
            val rodLevel = userInfo.getFishInfo().rodLevel
            event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, "你的鱼竿等级为%s级", rodLevel))
        }
    }

    @MessageAuthorize(
        text = ["钓鱼", "抛竿"],
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun fishing(event: GroupMessageEvent) {
        Log.info("钓鱼指令")
        val subject = event.subject
        val sender = event.sender
        val message = event.message
        val messageDate = Date(event.time.toLong() * 1000L)

        val userInfo = UserManager.getUserInfo(sender)
        val fishInfo = userInfo.getFishInfo()
        val fishTitle = TitleManager.checkTitleIsOnEnable(userInfo, TitleCode.FISHING)

        if (checkAndProcessFishing(userInfo, fishTitle, fishInfo, subject, message)) return

        if (UserStatusManager.checkUserNotInHome(userInfo) && !UserStatusManager.checkUserInFishpond(userInfo)) {
            val msg = when {
                UserStatusManager.checkUserInHospital(userInfo) -> "你还在医院躺着咧，怎么钓鱼?"
                UserStatusManager.checkUserInPrison(userInfo) -> "在监狱就不要想钓鱼的事了..."
                else -> "你当前不在可以钓鱼的地方"
            }
            subject.sendMessage(MessageUtil.formatMessageChain(message, msg))
            playerCooling.remove(userInfo.qq)
            return
        }

        UserStatusManager.moveFishpond(userInfo, 0)
        val fishPond = fishInfo.getFishPond(subject)

        if (fishInfo.rodLevel < fishPond.minLevel) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你的鱼竿等级太低了，升级升级鱼竿再来吧！"))
            playerCooling.remove(userInfo.qq)
            return
        }

        if (fishInfo.status) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你已经在钓鱼了!"))
            playerCooling.remove(userInfo.qq)
            return
        }

        val fishStartEvent = FishStartEvent(userInfo, fishInfo).broadcast()
        val fishBait = fishStartEvent.fishBait

        if (fishBait == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你没有鱼饵怎么钓？"))
            fishInfo.switchStatus()
            playerCooling.remove(userInfo.qq)
            return
        }

        subject.sendMessage(
            " ${userInfo.name}开始钓鱼\n" +
                    "鱼饵:${fishBait.name}\n" +
                    "鱼塘:${fishPond.name}\n" +
                    "等级:${fishPond.pondLevel}\n" +
                    "最低鱼竿等级:${fishPond.minLevel}\n" +
                    (fishPond.description ?: "")
        )
        Log.info("${userInfo.name}开始钓鱼")

        val pull = if (fishTitle) RandomUtil.randomInt(10, 101) else RandomUtil.randomInt(30, 151)
        val offset = RandomUtil.randomInt(2, 6)
        val prompt = pull - offset
        val planTime = DateUtil.offsetSecond(messageDate, pull)

        val reminderJob = launch {
            delay(prompt * 1000L)
            if (fishInfo.status && HuYanEconomy.PLUGIN_STATUS) {
                subject.sendMessage(MessageUtil.formatMessageChain(userInfo.qq, "浮漂动了!"))
            }
        }

        var resultTime: Date? = null
        try {
            withTimeoutOrNull(180 * 1000L) {
                while (isActive) {
                    val nextMessage = withContext(Dispatchers.IO) {
                        MessageUtil.INSTANCE.nextUserForGroupMessageEventSync(subject.id, sender.id, 180)
                    } ?: break

                    if (Pattern.matches("[拉起!！]", nextMessage.message.contentToString())) {
                        resultTime = Date()
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("钓鱼消息等待异常", e)
        } finally {
            reminderJob.cancel()
        }

        if (resultTime == null) {
            fishInfo.switchStatus()
            subject.sendMessage(MessageUtil.formatMessageChain(userInfo.qq, "你的鱼跑了!!!"))
            return
        }

        val between = DateUtil.between(planTime, resultTime, DateUnit.MS, true)
        val surprise = between <= 500
        var maxDifficulty = fishStartEvent.maxDifficulty ?: 1
        var maxGrade = fishStartEvent.maxGrade ?: 1

        when {
            between <= 500 -> maxDifficulty *= 2
            between <= 2000 -> maxDifficulty *= 2
            between <= 6000 -> maxGrade /= 2
            else -> if (failedFishing(userInfo, sender, subject, fishInfo)) return
        }

        val evolution =
            if (RandomUtil.randomInt(0, 101) >= 70) RandomUtil.randomFloat(0.5f, 0.8f) else RandomUtil.randomFloat(
                0f,
                0.5f
            )
        val minGrade = Math.round(evolution * fishBait.level).toInt()

        val fishRoll = FishRollEvent(
            userInfo, fishInfo, fishPond, fishBait, between / 1000f, evolution,
            maxGrade, minGrade, maxDifficulty, fishStartEvent.minDifficulty ?: 1, surprise
        ).broadcast()

        val fish = fishRoll.fish ?: run {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "卧槽，脱线了！"))
            fishInfo.switchStatus()
            return
        }

        val dimensions = fish.getSurprise(surprise, evolution)
        val money = (fish.price * dimensions).toDouble()
        val userMoney = money * (1 - fishPond.rebate)
        val pondMoney = money * fishPond.rebate

        if (EconomyUtil.plusMoneyToUser(sender, userMoney) &&
            EconomyUtil.plusMoneyToPluginBankForId(fishPond.code, fishPond.description ?: "", pondMoney)
        ) {
            fishPond.addNumber()
            val format =
                "\n起竿咯！\n${fish.name}\n等级:${fish.level}\n单价:${fish.price}\n尺寸:$dimensions\n总金额:$money\n${fish.description}"
            subject.sendMessage(MessageChainBuilder().append(At(userInfo.qq)).append(format).build())
        } else {
            subject.sendMessage("钓鱼失败!")
            playerCooling.remove(userInfo.qq)
        }

        fishInfo.switchStatus()
        HibernateFactory.merge(
            FishRanking(
                userInfo.qq,
                userInfo.name ?: "",
                dimensions,
                money,
                fishInfo.rodLevel,
                fish,
                fishPond
            )
        )
        UserStatusManager.moveHome(userInfo)
        TitleManager.checkFishTitle(userInfo, subject)
    }

    @MessageAuthorize(
        text = ["刷新钓鱼"],
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN],
        groupPermissions = [EconPerm.FISH_PERM]
    )
    suspend fun refresh(event: MessageEvent) {
        val status = HibernateFactory.getSessionFactory()?.fromTransaction { session ->
            try {
                val builder = session.criteriaBuilder
                val query = builder.createQuery(FishInfo::class.java)
                val from = query.from(FishInfo::class.java)
                query.select(from).where(builder.equal(from.get<Boolean>("status"), true))

                session.createQuery(query).list().forEach {
                    it.status = false
                    session.merge(it)
                }
                true
            } catch (e: Exception) {
                false
            }
        } ?: false
        playerCooling.clear()
        val msg = if (status) "钓鱼状态刷新成功!" else "钓鱼状态刷新失败!"
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

    @MessageAuthorize(text = ["开启 钓鱼"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun startFish(event: GroupMessageEvent) {
        val group = event.group
        val user = UserUtil.group(group.id)
        val util = PermUtil

        if (util.checkUserHasPerm(user, EconPerm.FISH_PERM)) {
            group.sendMessage("本群的钓鱼已经开启了!")
            return
        }

        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.FISH_PERM_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群钓鱼开启成功!"))
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群钓鱼开启失败!"))
        }
    }

    @MessageAuthorize(text = ["关闭 钓鱼"], userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN])
    suspend fun offFish(event: GroupMessageEvent) {
        val group = event.group
        val user = UserUtil.group(group.id)
        val util = PermUtil

        if (!util.checkUserHasPerm(user, EconPerm.FISH_PERM)) {
            group.sendMessage("本群的钓鱼已经关闭了!")
            return
        }

        util.talkPermGroupByName(EconPerm.GROUP.FISH_PERM_GROUP)?.apply {
            getUsers().remove(user)
            save()
        }
        group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群钓鱼关闭成功!"))
    }

    @MessageAuthorize(text = ["鱼塘等级"], groupPermissions = [EconPerm.FISH_PERM])
    suspend fun viewFishPond(event: GroupMessageEvent) {
        val group = event.group
        val userInfo = UserManager.getUserInfo(event.sender)
        val fishPond = userInfo.getFishInfo().getFishPond(group)
        val level = fishPond.pondLevel
        val value = FishPondLevelConstant.values()[level - 1]
        val money = fishPond.getFishPondMoney()

        group.sendMessage(
            MessageUtil.formatMessageChain(
                event.message,
                "当前鱼塘信息:%n" +
                        "鱼塘名称:%s%n" +
                        "鱼塘等级:%d%n" +
                        "鱼塘钓鱼次数:%d%n" +
                        "鱼塘最低鱼竿等级:%d%n" +
                        "鱼塘升级所需金额:%d%n" +
                        "鱼塘金额:%.1f%n" +
                        "鱼塘升级进度:%.1f%%",
                fishPond.name ?: "",
                level,
                fishPond.number,
                fishPond.minLevel,
                value.amount,
                money,
                (money / value.amount * 100)
            )
        )
    }

    private suspend fun checkAndProcessFishing(
        userInfo: UserInfo,
        isFishing: Boolean,
        fishInfo: FishInfo,
        subject: Contact,
        chain: MessageChain,
    ): Boolean {
        val qq = userInfo.qq
        if (isProcessing.putIfAbsent(qq, AtomicBoolean(true)) != null) {
            subject.sendMessage(MessageUtil.formatMessageChain(chain, "请稍后再试!"))
            return true
        }

        return try {
            playerCooling[qq]?.let { lastDate ->
                val between = DateUtil.between(lastDate, Date(), DateUnit.MINUTE, true)
                var expired = if (isFishing) 5 else (10 * 60 - fishInfo.rodLevel * 3) / 60

                FactorManager.getUserFactor(userInfo).getBuffValue(FunctionProps.RED_EYES)?.let { buff ->
                    if (DateUtil.between(Date(), DateUtil.parse(buff), DateUnit.MINUTE) <= 60) {
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
                            "你还差%s分钟来抛第二杆!",
                            expired - between
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

    private suspend fun failedFishing(userInfo: UserInfo, user: User, subject: Contact, fishInfo: FishInfo): Boolean {
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
}
