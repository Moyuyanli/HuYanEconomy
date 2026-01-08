package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.HuYanEconomy
import cn.chahuyun.economy.action.UserStatusAction
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.constant.FishPondLevelConstant
import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.entity.fish.FishRanking
import cn.chahuyun.economy.fish.FishRollEvent
import cn.chahuyun.economy.fish.FishStartEvent
import cn.chahuyun.economy.manager.GamesManager
import cn.chahuyun.economy.manager.TitleManager
import cn.chahuyun.economy.manager.UserCoreManager
import cn.chahuyun.economy.model.fish.FishBait
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.prop.PropsManager
import cn.chahuyun.economy.repository.FishRepository
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import kotlinx.coroutines.*
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChainBuilder
import java.util.*
import java.util.regex.Pattern
import kotlin.math.roundToInt

object GamesUsecase {

    /**
     * 自定义钓鱼事件：开始钓鱼时自动消耗/选择鱼饵。
     * （原逻辑在 `action.GamesAction` companion 中）
     */
    suspend fun fishStart(event: FishStartEvent) {
        val userInfo = event.userInfo
        val toRemove = mutableListOf<Long>()

        for (backpack in userInfo.backpacks) {
            if (event.fishBait == null && backpack.propKind == cn.chahuyun.economy.constant.PropsKind.fishBait) {
                val bait = try {
                    PropsManager.getProp(
                        backpack,
                        FishBait::class.java
                    )
                } catch (e: Exception) {
                    if (e.message == "该道具数据不存在") {
                        toRemove.add(backpack.propId ?: 0L)
                        continue
                    } else throw e
                }

                when {
                    bait.num > 1 -> {
                        PropsManager.useProp(
                            backpack, UseEvent(userInfo.user, userInfo.group, userInfo)
                        )
                        event.fishBait = bait
                    }

                    bait.num == 1 -> {
                        toRemove.add(backpack.propId ?: 0L)
                        event.fishBait = bait.copyProp()
                    }

                    else -> {
                        event.fishBait = FishBait().apply {
                            level = 1
                            quality = 0.01f
                            name = "空钩"
                        }
                        cn.chahuyun.economy.manager.BackpackManager.delPropToBackpack(userInfo, backpack.propId ?: 0L)
                    }
                }
            }
        }

        toRemove.forEach { cn.chahuyun.economy.manager.BackpackManager.delPropToBackpack(userInfo, it) }

        event.fishBait?.let {
            event.maxDifficulty = event.calculateMaxDifficulty()
            event.minDifficulty = event.calculateMinDifficulty()
            event.maxGrade = event.calculateMaxGrade()
        }
    }

    /**
     * 自定义钓鱼事件：抽取鱼结果。
     * （原逻辑在 `action.GamesAction` companion 中）
     */
    fun fishRoll(event: FishRollEvent) {
        val minDifficulty = event.minDifficulty.coerceAtLeast(1)
        val maxDifficulty = event.maxDifficulty
        val minGrade = event.minGrade.coerceAtLeast(1)
        val maxGrade = event.maxGrade
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

    suspend fun fishing(scope: CoroutineScope, event: GroupMessageEvent) {
        Log.info("钓鱼指令")
        val subject = event.subject
        val sender = event.sender
        val message = event.message
        val messageDate = Date(event.time.toLong() * 1000L)

        val userInfo = UserCoreManager.getUserInfo(sender)
        val fishInfo = userInfo.getFishInfo()
        val fishTitle = TitleManager.checkTitleIsOnEnable(userInfo, TitleCode.FISHING)

        if (GamesManager.checkAndProcessFishing(userInfo, fishTitle, fishInfo, subject, message)) return

        if (UserStatusAction.checkUserNotInHome(userInfo) && !UserStatusAction.checkUserInFishpond(userInfo)) {
            val msg = when {
                UserStatusAction.checkUserInHospital(userInfo) -> "你还在医院躺着咧，怎么钓鱼?"
                UserStatusAction.checkUserInPrison(userInfo) -> "在监狱就不要想钓鱼的事了..."
                else -> "你当前不在可以钓鱼的地方"
            }
            subject.sendMessage(MessageUtil.formatMessageChain(message, msg))
            GamesManager.removeCooling(userInfo.qq)
            return
        }

        UserStatusAction.moveFishpond(userInfo, 0)
        val fishPond = fishInfo.getFishPond(subject)

        if (fishInfo.rodLevel < fishPond.minLevel) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你的鱼竿等级太低了，升级升级鱼竿再来吧！"))
            GamesManager.removeCooling(userInfo.qq)
            return
        }

        if (fishInfo.status) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你已经在钓鱼了!"))
            GamesManager.removeCooling(userInfo.qq)
            return
        }

        val fishStartEvent = FishStartEvent(userInfo, fishInfo).broadcast()
        val fishBait = fishStartEvent.fishBait

        if (fishBait == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你没有鱼饵怎么钓？"))
            fishInfo.switchStatus()
            GamesManager.removeCooling(userInfo.qq)
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

        val reminderJob = scope.launch {
            delay(prompt * 1000L)
            if (fishInfo.status && HuYanEconomy.PLUGIN_STATUS) {
                subject.sendMessage(MessageUtil.formatMessageChain(userInfo.qq, "浮漂动了!"))
            }
        }

        var resultTime: Date? = null
        try {
            withTimeoutOrNull(180 * 1000L) {
                while (currentCoroutineContext().isActive) {
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
            else -> if (GamesManager.failedFishing(userInfo, sender, subject, fishInfo)) return
        }

        val evolution =
            if (RandomUtil.randomInt(0, 101) >= 70) RandomUtil.randomFloat(0.5f, 0.8f) else RandomUtil.randomFloat(
                0f,
                0.5f
            )
        val minGrade = (evolution * fishBait.level).roundToInt()

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

        if (
            EconomyUtil.plusMoneyToUser(sender, userMoney) &&
            EconomyUtil.plusMoneyToPluginBankForId(fishPond.code, fishPond.description ?: "", pondMoney)
        ) {
            fishPond.addNumber()
            val format =
                "\n起竿咯！\n${fish.name}\n等级:${fish.level}\n单价:${fish.price}\n尺寸:$dimensions\n总金额:$money\n${fish.description}"
            subject.sendMessage(MessageChainBuilder().append(At(userInfo.qq)).append(format).build())
        } else {
            subject.sendMessage("钓鱼失败!")
            GamesManager.removeCooling(userInfo.qq)
        }

        fishInfo.switchStatus()
        FishRepository.saveRanking(
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
        UserStatusAction.moveHome(userInfo)
        TitleManager.checkFishTitle(userInfo, subject)
    }

    suspend fun refresh(event: MessageEvent) {
        val status = FishRepository.resetAllFishingStatus()
        GamesManager.clearCooling()
        val msg = if (status) "钓鱼状态刷新成功!" else "钓鱼状态刷新失败!"
        event.subject.sendMessage(MessageUtil.formatMessageChain(event.message, msg))
    }

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

    suspend fun offFish(event: GroupMessageEvent) {
        val group = event.group
        val user = UserUtil.group(group.id)
        val util = PermUtil

        if (!util.checkUserHasPerm(user, EconPerm.FISH_PERM)) {
            group.sendMessage("本群的钓鱼已经关闭了!")
            return
        }

        util.takePermGroupByName(EconPerm.GROUP.FISH_PERM_GROUP).apply {
            users.remove(user)
            save()
        }
        group.sendMessage(MessageUtil.formatMessageChain(event.message, "本群钓鱼关闭成功!"))
    }

    suspend fun viewFishPond(event: GroupMessageEvent) {
        val group = event.group
        val userInfo = UserCoreManager.getUserInfo(event.sender)
        val fishPond = userInfo.getFishInfo().getFishPond(group)
        val level = fishPond.pondLevel
        val value = FishPondLevelConstant.entries[level - 1]
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
}


