package cn.chahuyun.economy.service

import cn.chahuyun.economy.data.repository.FishRepository
import cn.chahuyun.economy.model.fish.*
import cn.chahuyun.economy.model.props.FunctionProps
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.utils.MessageUtil
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.MessageChain
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object FishingRuntimeService {
    private val playerCooling = ConcurrentHashMap<Long, Date>()
    private val isProcessing = ConcurrentHashMap<Long, AtomicBoolean>()

    fun getFishingCooldownText(userInfo: UserInfoDto, isFishingTitle: Boolean, fishInfo: FishInfoDto): String {
        val lastDate = playerCooling[userInfo.qq] ?: return "可钓鱼"
        val between = DateUtil.between(lastDate, Date(), DateUnit.MINUTE, true)
        var expired = calculateExpiredMinutes(isFishingTitle, fishInfo)

        EconomyFactorService.getUserBuff(userInfo, FunctionProps.RED_EYES)?.let { buff ->
            if (DateUtil.between(DateUtil.parse(buff), Date(), DateUnit.MINUTE) <= 60) {
                expired -= (expired * 0.8).toInt()
            }
        }

        val remaining = expired - between
        return if (remaining > 0) "还需 ${remaining} 分钟" else "可钓鱼"
    }

    suspend fun checkAndProcessFishing(
        userInfo: UserInfoDto,
        isFishingTitle: Boolean,
        fishInfo: FishInfoDto,
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
                var expired = calculateExpiredMinutes(isFishingTitle, fishInfo)

                EconomyFactorService.getUserBuff(userInfo, FunctionProps.RED_EYES)?.let { buff ->
                    if (DateUtil.between(DateUtil.parse(buff), Date(), DateUnit.MINUTE) <= 60) {
                        expired -= (expired * 0.8).toInt()
                    } else {
                        EconomyFactorService.clearUserBuff(userInfo, FunctionProps.RED_EYES)
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

    fun removeCooling(qq: Long) {
        playerCooling.remove(qq)
    }

    fun clearCooling() {
        playerCooling.clear()
    }

    suspend fun failedFishing(
        userInfo: UserInfoDto,
        user: User,
        subject: Contact,
        fishInfo: FishInfoDto,
    ): Boolean {
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
                EconomyUserStatusService.movePrison(userInfo, 60)
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

    fun resetAllFishingStatus(): Boolean =
        FishRepository.resetAllFishingStatus()

    fun saveRanking(
        qq: Long,
        name: String,
        dimensions: Int,
        money: Double,
        fishRodLevel: Int,
        fish: FishDto,
        fishPond: FishPondDto,
    ): FishRankingDto =
        FishRepository.saveRanking(qq, name, dimensions, money, fishRodLevel, fish, fishPond)

    private fun calculateExpiredMinutes(isFishingTitle: Boolean, fishInfo: FishInfoDto): Int =
        if (isFishingTitle) 5 else (10 * 60 - fishInfo.rodLevel * 3) / 60
}
