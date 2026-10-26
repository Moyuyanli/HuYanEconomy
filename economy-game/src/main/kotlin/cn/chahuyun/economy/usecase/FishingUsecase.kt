package cn.chahuyun.economy.usecase

import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.constant.FishingBaitConstant
import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.data.repository.UserFishingInfoRepository
import cn.chahuyun.economy.entity.fishing.FishEntity
import cn.chahuyun.economy.entity.fishing.UserFishingInfoEntity
import cn.chahuyun.economy.entity.fishing.UserPondEntity
import cn.chahuyun.economy.games.GamesReplySupport
import cn.chahuyun.economy.image.FishingInfoImageRenderer
import cn.chahuyun.economy.image.model.FishingInfoCard
import cn.chahuyun.economy.model.fish.FishingBait
import cn.chahuyun.economy.model.user.UserInfoDto
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
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import java.math.BigDecimal
import java.util.*
import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * 钓鱼玩法用例。
 *
 * 沿用现网玩法：抛竿后等待玩家在限定时间内发送“拉起/!”，
 * 根据响应时间判定惊喜尺寸、难度加成，随后抽取鱼类、计算尺寸与结算，
 * 资金变动全部通过 economy-base（用户账户 `u<qq>`、鱼塘账户 `f<pondId>`）。
 */
object FishingUsecase {

    suspend fun fishing(scope: CoroutineScope, event: GroupMessageEvent) {
        Log.info("钓鱼指令")
        val group = event.group
        val sender = event.sender
        val message = event.message
        val messageDate = Date(event.time.toLong() * 1000L)

        val userInfo = EconomyUserService.getOrCreate(sender)
        val userId = FishingIdentity.userId(userInfo)
        val fishInfo = UserFishingInfoRepository.getOrCreate(userId)
        val fishTitle = EconomyTitleService.isEnabled(userInfo, TitleCode.FISHING)

        val remaining = FishingRuntimeService.remainingCooldownMillis(userInfo.qq, fishInfo.rodLevel, fishTitle)
        if (remaining > 0) {
            GamesReplySupport.reply(group, message, "你还差${FishingRuntimeService.minutesRoundedUp(remaining)}分钟来抛第二杆!")
            return
        }

        if (EconomyUserStatusService.isNotHome(userInfo) && !EconomyUserStatusService.isInFishpond(userInfo)) {
            val msg = when {
                EconomyUserStatusService.isInHospital(userInfo) -> "你还在医院躺着咧，怎么钓鱼?"
                EconomyUserStatusService.isInPrison(userInfo) -> "在监狱就不要想钓鱼的事了..."
                else -> "你当前不在可以钓鱼的地方"
            }
            GamesReplySupport.reply(group, message, msg)
            return
        }

        if (fishInfo.rodLevel <= 0) {
            GamesReplySupport.reply(group, message, "你还没有鱼竿，先购买鱼竿吧!")
            return
        }

        if (!FishingRuntimeService.tryBeginSession(userInfo.qq)) {
            GamesReplySupport.reply(group, message, "请稍后再试!")
            return
        }

        try {
            EconomyUserStatusService.moveFishpond(userInfo, 0)
            FishingRuntimeService.markFishing(userInfo.qq)

            val pond = FishingPondService.resolveDefaultPond(fishInfo, userId, userInfo.name)
            val baitConstant = FishingBaitConstant.ofId(fishInfo.defaultBaitItemId)
            val bait = FishingBait.of(baitConstant)

            val minDifficultyBase = Math.round(fishInfo.rodLevel * bait.quality)
            val maxGradeBase = FishingRodUpgradeRules.fishingLevel(fishInfo.rodLevel)

            GamesReplySupport.plain(event, FishingMessageBuilder.startFishing(userInfo.name, bait, pond))
            Log.info("${userInfo.name}开始钓鱼")

            val pull = if (fishTitle) RandomUtil.randomInt(10, 101) else RandomUtil.randomInt(30, 151)
            val offset = RandomUtil.randomInt(2, 6)
            val prompt = (pull - offset).coerceAtLeast(1)
            val planTime = DateUtil.offsetSecond(messageDate, pull)

            val reminderJob = scope.launch {
                delay(prompt * 1000L)
                if (EconomyRuntime.pluginStatus) {
                    GamesReplySupport.reply(group, userInfo.qq, "浮漂动了!")
                }
            }

            var resultTime: Date? = null
            try {
                withTimeoutOrNull(180 * 1000L) {
                    while (currentCoroutineContext().isActive) {
                        val nextMessage = MessageUtil.INSTANCE
                            .nextUserForGroupMessageEvent(group.id, sender.id, 180) ?: break
                        if (Pattern.matches("[拉起!！]", nextMessage.message.contentToString().trim())) {
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

            val landed = resultTime
            if (landed == null) {
                GamesReplySupport.reply(group, userInfo.qq, "你的鱼跑了!!!")
                return
            }

            val between = DateUtil.between(planTime, landed, DateUnit.MS, true)
            val surprise = between <= 500
            var maxDifficulty = fishInfo.rodLevel * bait.level
            var maxGrade = maxGradeBase

            when {
                between <= 500 -> maxDifficulty *= 2
                between <= 2000 -> maxDifficulty *= 2
                between <= 6000 -> maxGrade /= 2
                else -> if (FishingRuntimeService.failedFishing(userInfo, group)) return
            }

            val evolution = if (RandomUtil.randomInt(0, 101) >= 70) {
                RandomUtil.randomFloat(0.5f, 0.8f)
            } else {
                RandomUtil.randomFloat(0f, 0.5f)
            }
            val minGrade = (evolution * bait.level).roundToInt()

            val fish = rollFish(pond, minDifficultyBase, maxDifficulty, minGrade, maxGrade)
            if (fish == null) {
                GamesReplySupport.reply(event, "卧槽，脱线了！")
                return
            }

            val dimensions = FishingSizeService.surpriseDimensions(fish, surprise, evolution)
            val totalMoney = fish.price.multiply(BigDecimal.valueOf(dimensions.toLong()))
            val feeAmount = totalMoney.multiply(BigDecimal.valueOf(FishingPondService.feeRate(pond)))
            val netIncome = totalMoney.subtract(feeAmount)

            if (FishingAccountService.addUser(userInfo.qq, netIncome) &&
                FishingAccountService.addPond(pond.id, feeAmount)
            ) {
                FishingPondService.incrementFishingCount(pond)
                FishingLogService.record(
                    userId = userId,
                    fish = fish,
                    pond = pond,
                    rodLevel = fishInfo.rodLevel,
                    size = dimensions,
                    price = totalMoney,
                    feeAmount = feeAmount,
                    netIncome = netIncome,
                    baitItemId = baitConstant?.id,
                )
                updateStatistics(fishInfo, dimensions, netIncome)
                GamesReplySupport.send(
                    event,
                    FishingMessageBuilder.success(userInfo.qq, fish, dimensions, totalMoney),
                )
                checkFishTitle(userInfo, userId, group)
            } else {
                Log.error("钓鱼:结算入账失败 qq=${userInfo.qq}")
                GamesReplySupport.plain(event, "钓鱼失败!")
            }
        } finally {
            FishingRuntimeService.endSession(userInfo.qq)
            EconomyUserStatusService.moveHome(userInfo)
        }
    }

    suspend fun refresh(event: MessageEvent) {
        FishingRuntimeService.resetAll()
        GamesReplySupport.reply(event, "钓鱼状态刷新成功!")
    }

    suspend fun startFish(event: GroupMessageEvent) {
        val group = event.group
        val user = UserUtil.group(group.id)
        val util = PermUtil

        if (util.checkUserHasPerm(user, EconPerm.FISH_PERM)) {
            GamesReplySupport.plain(group, "本群的钓鱼已经开启了!")
            return
        }
        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.FISH_PERM_GROUP)) {
            GamesReplySupport.reply(group, event.message, "本群钓鱼开启成功!")
        } else {
            GamesReplySupport.reply(group, event.message, "本群钓鱼开启失败!")
        }
    }

    suspend fun offFish(event: GroupMessageEvent) {
        val group = event.group
        val user = UserUtil.group(group.id)
        val util = PermUtil

        if (!util.checkUserHasPerm(user, EconPerm.FISH_PERM)) {
            GamesReplySupport.plain(group, "本群的钓鱼已经关闭了!")
            return
        }
        util.takePermGroupByName(EconPerm.GROUP.FISH_PERM_GROUP).apply {
            users.remove(user)
            save()
        }
        GamesReplySupport.reply(group, event.message, "本群钓鱼关闭成功!")
    }

    suspend fun viewFishPond(event: GroupMessageEvent) {
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val userId = FishingIdentity.userId(userInfo)
        val fishInfo = UserFishingInfoRepository.getOrCreate(userId)
        val pond = FishingPondService.resolveDefaultPond(fishInfo, userId, userInfo.name)
        val money = FishingAccountService.pondBalance(pond.id)

        GamesReplySupport.reply(event.group, event.message, FishingMessageBuilder.pondInfo(pond, money))
    }

    suspend fun viewFishingInfo(event: GroupMessageEvent) {
        val card = buildFishingInfoCard(event)
        try {
            ImageMessageUtil.sendQuotedImage(event.subject, event.message, FishingInfoImageRenderer.render(card))
        } catch (e: Exception) {
            Log.error("钓鱼信息图片生成或发送失败", e)
            GamesReplySupport.reply(event, formatFishingInfoText(card))
        }
    }

    suspend fun viewFishingInfoText(event: GroupMessageEvent) {
        GamesReplySupport.reply(event, formatFishingInfoText(buildFishingInfoCard(event)))
    }

    suspend fun createPond(event: GroupMessageEvent, name: String) {
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val userId = FishingIdentity.userId(userInfo)
        val msg = EconomyRuntime.msgConfig

        val pond = FishingPondService.createNamedPond(userId, name)
        if (pond == null) {
            GamesReplySupport.reply(event, msg.pondNameDuplicated)
            return
        }
        GamesReplySupport.reply(event, msg.createPondSuccess.format(name))
    }

    suspend fun buyBait(event: GroupMessageEvent, code: String) {
        val userInfo = EconomyUserService.getOrCreate(event.sender)
        val userId = FishingIdentity.userId(userInfo)
        val bait = FishingBaitConstant.ofCode(code)
        if (bait == null) {
            GamesReplySupport.reply(event, "没有找到鱼饵:$code")
            return
        }

        val price = BigDecimal.valueOf(bait.price)
        if (FishingAccountService.userBalance(userInfo.qq) < price) {
            GamesReplySupport.reply(event, "你的金币不足以购买该鱼饵(需要 ${MoneyFormatUtil.format(bait.price)})!")
            return
        }
        if (FishingAccountService.deductUser(userInfo.qq, price)) {
            val fishInfo = UserFishingInfoRepository.getOrCreate(userId)
            fishInfo.defaultBaitItemId = bait.id
            fishInfo.touch()
            UserFishingInfoRepository.save(fishInfo)
            GamesReplySupport.reply(event, "购买鱼饵成功:${bait.displayName}(Lv.${bait.level})")
        } else {
            GamesReplySupport.reply(event, "购买鱼饵失败!")
        }
    }

    // ---- 内部逻辑 ----

    private fun rollFish(
        pond: UserPondEntity,
        minDifficulty: Int,
        maxDifficulty: Int,
        minGrade: Int,
        maxGrade: Int,
    ): FishEntity? {
        val maxFishLevel = FishingPondService.maxFishLevel(pond)
        val upperGrade = maxGrade.coerceAtMost(maxFishLevel).coerceAtLeast(1)
        val lowerGrade = minGrade.coerceAtLeast(1).coerceAtMost(upperGrade)
        var rank = RandomUtil.randomInt(lowerGrade, upperGrade + 1)
        val minDiff = minDifficulty.coerceAtLeast(1)

        repeat(64) {
            val difficulty = RandomUtil.randomInt(minDiff, (maxDifficulty + 1).coerceAtLeast(minDiff + 1))
            val levelFishList = FishingCatalogService.levelFishList(rank).filter { it.level <= maxFishLevel }
            if (levelFishList.isEmpty()) {
                if (rank > 1) {
                    rank--
                    return@repeat
                }
                return FishingCatalogService.levelRangeFish(1, maxFishLevel).firstOrNull()
            }
            val collect = levelFishList.filter { it.technique <= difficulty }.sortedBy { it.description }
            if (collect.isEmpty()) {
                if (rank > 1) {
                    rank--
                    return@repeat
                }
                return levelFishList.firstOrNull()
            }
            return collect[RandomUtil.randomInt(0, collect.size.coerceAtMost(4))]
        }
        return FishingCatalogService.levelRangeFish(1, maxFishLevel).firstOrNull()
    }

    private fun updateStatistics(fishInfo: UserFishingInfoEntity, dimensions: Int, netIncome: BigDecimal) {
        fishInfo.totalFishingCount += 1
        if (dimensions > fishInfo.maxFishSize) fishInfo.maxFishSize = dimensions
        if (netIncome > fishInfo.maxFishPrice) fishInfo.maxFishPrice = netIncome
        fishInfo.touch()
        UserFishingInfoRepository.save(fishInfo)
    }

    private suspend fun checkFishTitle(userInfo: UserInfoDto, userId: Long, subject: Contact) {
        if (EconomyTitleService.exists(userInfo, TitleCode.FISHING)) return
        if (FishingLogService.globalTopUserId() != userId) return
        if (EconomyTitleService.grant(userInfo, TitleCode.FISHING)) {
            subject.sendMessage(
                MessageUtil.formatMessageChain(userInfo.qq, "恭喜你获得钓鱼排行榜榜首!获得 [钓鱼佬] 称号!")
            )
        }
    }

    private suspend fun buildFishingInfoCard(event: GroupMessageEvent): FishingInfoCard {
        val sender = event.sender
        val userInfo = EconomyUserService.getOrCreate(sender)
        val userId = FishingIdentity.userId(userInfo)
        val fishInfo = UserFishingInfoRepository.getOrCreate(userId)
        val pond = FishingPondService.resolveDefaultPond(fishInfo, userId, userInfo.name)
        val biggest = FishingLogService.recordsOf(userId).maxByOrNull { it.size }

        return FishingInfoCard(
            owner = "${userInfo.name}(${userInfo.qq})",
            rodLevel = if (fishInfo.rodLevel <= 0) "未购买" else "Lv.${fishInfo.rodLevel}",
            currentPond = "${pond.name}(Lv.${pond.level})",
            maxFishSize = "${fishInfo.maxFishSize}cm",
            maxNetIncome = MoneyFormatUtil.format(fishInfo.maxFishPrice.toDouble()),
            totalCount = fishInfo.totalFishingCount.toString(),
            biggestFish = biggest?.let { "${it.fishName} ${it.size}cm" } ?: "暂无记录",
            biggestFishDetail = biggest?.let {
                "鱼等级 Lv.${it.fishLevel} / 价值 ${MoneyFormatUtil.format(it.price.toDouble())} / 鱼塘 ${it.pondName}"
            } ?: "还没有成功上鱼，去抛第一竿吧。",
        )
    }

    private fun formatFishingInfoText(card: FishingInfoCard): String = buildString {
        append("钓鱼信息：${card.owner}\n")
        append("鱼竿等级：${card.rodLevel}\n")
        append("当前鱼塘：${card.currentPond}\n")
        append("最大鱼尺寸：${card.maxFishSize}\n")
        append("单条最高收益：${card.maxNetIncome}\n")
        append("累计钓鱼次数：${card.totalCount}\n")
        append("最大渔获：${card.biggestFish}\n")
        append(card.biggestFishDetail)
    }
}
