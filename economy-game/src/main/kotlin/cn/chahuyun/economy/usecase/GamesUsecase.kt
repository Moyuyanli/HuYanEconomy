package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.data.repository.FishRepository
import cn.chahuyun.economy.fish.FishRollEvent
import cn.chahuyun.economy.fish.FishStartEvent
import cn.chahuyun.economy.image.FishingInfoImageRenderer
import cn.chahuyun.economy.image.model.FishingInfoCard
import cn.chahuyun.economy.model.fish.*
import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.model.user.getFishInfo
import cn.chahuyun.economy.model.user.group
import cn.chahuyun.economy.model.user.user
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.service.*
import cn.chahuyun.economy.utils.ImageMessageUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MessageUtil
import cn.chahuyun.economy.utils.MoneyFormatUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import kotlinx.coroutines.*
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
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
                    EconomyInventoryService.getProp(
                        backpack,
                        FishBait::class.java
                    )
                } catch (e: Exception) {
                    if (e.message == "该道具数据不存在") {
                        toRemove.add(backpack.propId)
                        continue
                    } else throw e
                }

                when {
                    bait.num > 1 -> {
                        EconomyInventoryService.useProp(
                            backpack, UseEvent(userInfo.user, userInfo.group, userInfo)
                        )
                        event.fishBait = bait
                    }

                    bait.num == 1 -> {
                        toRemove.add(backpack.propId)
                        event.fishBait = bait.copyProp()
                    }

                    else -> {
                        event.fishBait = FishBait().apply {
                            level = 1
                            quality = 0.01f
                            name = "空钩"
                        }
                        EconomyInventoryService.deleteProp(userInfo, backpack.propId)
                    }
                }
            }
        }

        toRemove.forEach { EconomyInventoryService.deleteProp(userInfo, it) }

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

        val userInfo = EconomyUserService.getOrCreate(sender)
        val fishInfo = userInfo.getFishInfo()
        val fishTitle = EconomyTitleService.isEnabled(userInfo, TitleCode.FISHING)

        if (FishingRuntimeService.checkAndProcessFishing(userInfo, fishTitle, fishInfo, subject, message)) return

        if (EconomyUserStatusService.isNotHome(userInfo) && !EconomyUserStatusService.isInFishpond(userInfo)) {
            val msg = when {
                EconomyUserStatusService.isInHospital(userInfo) -> "你还在医院躺着咧，怎么钓鱼?"
                EconomyUserStatusService.isInPrison(userInfo) -> "在监狱就不要想钓鱼的事了..."
                else -> "你当前不在可以钓鱼的地方"
            }
            GameUsecaseReplySupport.reply(event, msg)
            FishingRuntimeService.removeCooling(userInfo.qq)
            return
        }

        EconomyUserStatusService.moveFishpond(userInfo, 0)
        val fishPond = fishInfo.getFishPond(subject)

        if (fishInfo.rodLevel < fishPond.minLevel) {
            GameUsecaseReplySupport.reply(event, "你的鱼竿等级太低了，升级升级鱼竿再来吧！")
            FishingRuntimeService.removeCooling(userInfo.qq)
            return
        }

        if (fishInfo.status) {
            GameUsecaseReplySupport.reply(event, "你已经在钓鱼了!")
            FishingRuntimeService.removeCooling(userInfo.qq)
            return
        }

        val fishStartEvent = FishStartEvent(userInfo, fishInfo).broadcast()
        val fishBait = fishStartEvent.fishBait

        if (fishBait == null) {
            GameUsecaseReplySupport.reply(event, "你没有鱼饵怎么钓？")
            fishInfo.switchStatus()
            FishingRuntimeService.removeCooling(userInfo.qq)
            return
        }

        GameUsecaseReplySupport.plain(
            event,
            FishingMessageBuilder.startFishing(userInfo.name, fishBait, fishPond)
        )
        Log.info("${userInfo.name}开始钓鱼")

        val pull = if (fishTitle) RandomUtil.randomInt(10, 101) else RandomUtil.randomInt(30, 151)
        val offset = RandomUtil.randomInt(2, 6)
        val prompt = pull - offset
        val planTime = DateUtil.offsetSecond(messageDate, pull)

        val reminderJob = scope.launch {
            delay(prompt * 1000L)
            if (fishInfo.status && EconomyRuntime.pluginStatus) {
                GameUsecaseReplySupport.reply(subject, userInfo.qq, "浮漂动了!")
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
            GameUsecaseReplySupport.reply(subject, userInfo.qq, "你的鱼跑了!!!")
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
            else -> if (FishingRuntimeService.failedFishing(userInfo, sender, subject, fishInfo)) return
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
            GameUsecaseReplySupport.reply(event, "卧槽，脱线了！")
            fishInfo.switchStatus()
            return
        }

        val dimensions = FishingSizeService.surpriseDimensions(fish, surprise, evolution)
        val money = (fish.price * dimensions).toDouble()
        val userMoney = money * (1 - fishPond.rebate)
        val pondMoney = money * fishPond.rebate

        if (
            EconomyAccountService.addWallet(sender, userMoney) &&
            EconomyAccountService.addPluginBank(fishPond.code, fishPond.description, pondMoney)
        ) {
            fishPond.addNumber()
            GameUsecaseReplySupport.send(event, FishingMessageBuilder.success(userInfo.qq, fish, dimensions, money))
        } else {
            GameUsecaseReplySupport.plain(event, "钓鱼失败!")
            FishingRuntimeService.removeCooling(userInfo.qq)
        }

        fishInfo.switchStatus()
        FishingRuntimeService.saveRanking(
            userInfo.qq,
            userInfo.name,
            dimensions,
            money,
            fishInfo.rodLevel,
            fish,
            fishPond
            )
        EconomyUserStatusService.moveHome(userInfo)
        EconomyTitleService.checkFishTitle(userInfo, subject)
    }

    suspend fun refresh(event: MessageEvent) {
        val status = FishingRuntimeService.resetAllFishingStatus()
        FishingRuntimeService.clearCooling()
        val msg = if (status) "钓鱼状态刷新成功!" else "钓鱼状态刷新失败!"
        GameUsecaseReplySupport.reply(event, msg)
    }

    suspend fun startFish(event: GroupMessageEvent) {
        val group = event.group
        val user = UserUtil.group(group.id)
        val util = PermUtil

        if (util.checkUserHasPerm(user, EconPerm.FISH_PERM)) {
            GameUsecaseReplySupport.plain(group, "本群的钓鱼已经开启了!")
            return
        }

        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.FISH_PERM_GROUP)) {
            GameUsecaseReplySupport.reply(event.group, event.message, "本群钓鱼开启成功!")
        } else {
            GameUsecaseReplySupport.reply(event.group, event.message, "本群钓鱼开启失败!")
        }
    }

    suspend fun offFish(event: GroupMessageEvent) {
        val group = event.group
        val user = UserUtil.group(group.id)
        val util = PermUtil

        if (!util.checkUserHasPerm(user, EconPerm.FISH_PERM)) {
            GameUsecaseReplySupport.plain(group, "本群的钓鱼已经关闭了!")
            return
        }

        util.takePermGroupByName(EconPerm.GROUP.FISH_PERM_GROUP).apply {
            users.remove(user)
            save()
        }
        GameUsecaseReplySupport.reply(event.group, event.message, "本群钓鱼关闭成功!")
    }

    suspend fun viewFishPond(event: GroupMessageEvent) {
        val group = event.group
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val fishPond = userInfo.getFishInfo().getFishPond(group)
        val money = fishPond.getFishPondMoney()

        GameUsecaseReplySupport.reply(
            group,
            event.message,
            FishingMessageBuilder.pondInfo(fishPond, money)
        )
    }

    suspend fun viewFishingInfo(event: GroupMessageEvent) {
        val card = buildFishingInfoCard(event)
        try {
            ImageMessageUtil.sendQuotedImage(event.subject, event.message, FishingInfoImageRenderer.render(card))
        } catch (e: Exception) {
            Log.error("钓鱼信息图片生成或发送失败", e)
            GameUsecaseReplySupport.reply(event, formatFishingInfoCard(card))
        }
    }

    suspend fun viewFishingInfoText(event: GroupMessageEvent) {
        GameUsecaseReplySupport.reply(event, formatFishingInfoCard(buildFishingInfoCard(event)))
    }

    private fun buildFishingInfoCard(event: GroupMessageEvent): FishingInfoCard {
        val sender = event.sender
        val userInfo = EconomyUserService.getOrCreate(sender)
        val fishInfo = userInfo.getFishInfo()
        val currentPond = fishInfo.getFishPond(event.group)
        val rankings = FishRepository.listRankingByQq(sender.id)
        val biggest = rankings.maxByOrNull { it.dimensions }
        val maxPondLevel = fishInfo.level
        val accessiblePonds = FishRepository.listFishPonds()
            .filter { it.minLevel <= maxPondLevel }
        val maxPond = accessiblePonds.maxWithOrNull(
            compareBy<cn.chahuyun.economy.entity.fish.FishPond> { it.pondLevel }
                .thenBy { it.minLevel }
        )
        val maxPondText = maxPond?.let { "${it.name}(Lv.${it.pondLevel})" } ?: "暂无可用鱼塘"

        return FishingInfoCard(
            owner = "${sender.nameCardOrNick}(${sender.id})",
            rodLevel = if (fishInfo.isFishRod) "Lv.${fishInfo.rodLevel}" else "未购买",
            maxPond = maxPondText,
            biggestFish = biggest?.let { "${it.fishName.ifBlank { "未知鱼" }} ${it.dimensions}cm" } ?: "暂无记录",
            biggestFishDetail = biggest?.let {
                "鱼等级 Lv.${it.fishLevel} / 价值 ${MoneyFormatUtil.format(it.money)} / 鱼塘 ${it.fishPondName.ifBlank { "未知鱼塘" }}"
            } ?: "还没有成功上鱼，去抛第一竿吧。",
            historyCount = rankings.size.toString(),
            successCount = rankings.size.toString(),
            currentPond = "${currentPond.name}(Lv.${currentPond.pondLevel})",
            currentPondDetail = "最低鱼竿等级 ${currentPond.minLevel} / 累计上鱼 ${currentPond.number} 次 / 鱼种 ${currentPond.fishCount} 个",
        )
    }

    private fun formatFishingInfoCard(card: FishingInfoCard): String = buildString {
        append("钓鱼信息：").append(card.owner).append('\n')
        append("鱼竿等级：").append(card.rodLevel).append('\n')
        append("最多钓鱼鱼塘：").append(card.maxPond).append('\n')
        append("最大的鱼：").append(card.biggestFish).append('\n')
        append(card.biggestFishDetail).append('\n')
        append("历史钓鱼次数：").append(card.historyCount).append('\n')
        append("上鱼次数：").append(card.successCount).append('\n')
        append("当前鱼塘：").append(card.currentPond).append('\n')
        append(card.currentPondDetail)
    }
}
