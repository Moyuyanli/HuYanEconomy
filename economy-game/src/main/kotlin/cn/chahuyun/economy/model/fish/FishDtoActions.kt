package cn.chahuyun.economy.model.fish

import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.service.FishPondQueryService
import cn.chahuyun.economy.service.FishRuntimeDataService
import cn.chahuyun.economy.service.FishingRankingMessageFormatter
import cn.chahuyun.economy.service.FishingRodUpgradeService
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.SingleMessage

object FishBehaviorService {

    fun getFishPond(group: Group): FishPondDto =
        FishRuntimeDataService.findOrCreatePond(group)

    fun updateRod(fishInfo: FishInfoDto, userInfo: UserInfoDto): SingleMessage =
        FishingRodUpgradeService.upgrade(fishInfo, userInfo)

    fun switchStatus(fishInfo: FishInfoDto) =
        FishRuntimeDataService.finishFishing(fishInfo)

    fun save(fishInfo: FishInfoDto): FishInfoDto =
        FishRuntimeDataService.saveFishInfo(fishInfo)

    fun getFishPondMoney(pond: FishPondDto): Double =
        FishPondQueryService.pondMoney(pond)

    fun getFishList(pond: FishPondDto, level: Int): List<FishDto> =
        FishPondQueryService.levelFishList(pond, level)

    fun addNumber(pond: FishPondDto) =
        FishRuntimeDataService.incrementPondCount(pond)

    fun save(pond: FishPondDto): FishPondDto =
        FishRuntimeDataService.saveFishPond(pond)

    fun getInfo(ranking: FishRankingDto, top: Int): SingleMessage {
        return FishingRankingMessageFormatter.rankingInfo(ranking, top)
    }
}

val FishInfoDto.level: Int
    get() = if (rodLevel == 0) 1 else rodLevel / 10 + 2

fun FishInfoDto.getFishPond(group: Group): FishPondDto =
    FishBehaviorService.getFishPond(group)

fun FishInfoDto.updateRod(userInfo: UserInfoDto): SingleMessage =
    FishBehaviorService.updateRod(this, userInfo)

fun FishInfoDto.switchStatus() =
    FishBehaviorService.switchStatus(this)

fun FishInfoDto.save(): FishInfoDto =
    FishBehaviorService.save(this)

fun FishPondDto.getFishPondMoney(): Double =
    FishBehaviorService.getFishPondMoney(this)

fun FishPondDto.getFishList(level: Int): List<FishDto> =
    FishBehaviorService.getFishList(this, level)

fun FishPondDto.addNumber() =
    FishBehaviorService.addNumber(this)

fun FishPondDto.save(): FishPondDto =
    FishBehaviorService.save(this)

fun FishRankingDto.getInfo(top: Int): SingleMessage =
    FishBehaviorService.getInfo(this, top)
