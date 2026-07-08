package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.usecase.FarmUsecase
import net.mamoe.mirai.event.events.GroupMessageEvent

@EventComponent
class FarmAction {

    @MessageAuthorize(text = ["我的农场"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun viewFarm(event: GroupMessageEvent) {
        FarmUsecase.viewFarm(event)
    }

    @MessageAuthorize(text = ["农场商店"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun viewShop(event: GroupMessageEvent) {
        FarmUsecase.viewShop(event)
    }

    @MessageAuthorize(text = ["农场仓库"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun viewWarehouse(event: GroupMessageEvent) {
        FarmUsecase.viewWarehouse(event)
    }

    @MessageAuthorize(
        text = ["购买种子 \\S+( \\d+)?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun buySeed(event: GroupMessageEvent) {
        FarmUsecase.buySeed(event)
    }

    @MessageAuthorize(
        text = ["(播种|种植) .+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun plant(event: GroupMessageEvent) {
        FarmUsecase.plant(event)
    }

    @MessageAuthorize(
        text = ["收获 .+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun harvest(event: GroupMessageEvent) {
        FarmUsecase.harvest(event)
    }

    @MessageAuthorize(
        text = ["卖出果实 .+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun sellFruits(event: GroupMessageEvent) {
        FarmUsecase.sellFruits(event)
    }

    @MessageAuthorize(text = ["升级农场"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun upgradeFarm(event: GroupMessageEvent) {
        FarmUsecase.upgradeFarm(event)
    }

    @MessageAuthorize(
        text = ["帮浇水.*"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun water(event: GroupMessageEvent) {
        FarmUsecase.water(event)
    }

    @MessageAuthorize(text = ["一键卖出"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun sellAll(event: GroupMessageEvent) {
        FarmUsecase.sellAll(event)
    }

    @MessageAuthorize(text = ["一键收获"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun harvestAll(event: GroupMessageEvent) {
        FarmUsecase.harvestAll(event)
    }

    @MessageAuthorize(
        text = ["一键播种 .+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun plantAll(event: GroupMessageEvent) {
        FarmUsecase.plantAll(event)
    }

    @MessageAuthorize(text = ["激活守护"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun activateShield(event: GroupMessageEvent) {
        FarmUsecase.activateShield(event)
    }

    @MessageAuthorize(text = ["黑市"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun blackMarket(event: GroupMessageEvent) {
        FarmUsecase.blackMarket(event)
    }
}
