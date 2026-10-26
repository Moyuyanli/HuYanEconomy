package cn.chahuyun.economy.action

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.games.farm.FarmUsecaseActions
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 农场指令入口。
 *
 * 只做 Mirai 指令匹配与权限校验，业务全部在 `games/farm/FarmUsecase` 的规则链里。
 */
@EventComponent
class FarmAction {

    /** 我的农场 / 农场信息：渲染农场详情图片。 */
    @MessageAuthorize(text = ["我的农场", "农场信息"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun viewFarm(event: GroupMessageEvent) {
        FarmUsecaseActions.view(event)
    }

    /** 我的农场# / 农场信息#：纯文本版，末尾 `#` 用于区分。 */
    @MessageAuthorize(text = ["我的农场#", "农场信息#"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun viewFarmText(event: GroupMessageEvent) {
        FarmUsecaseActions.viewText(event)
    }

    /** 农场详情：与「我的农场」同一张图片，保留别名。 */
    @MessageAuthorize(text = ["农场详情"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun viewFarmDetail(event: GroupMessageEvent) {
        FarmUsecaseActions.detail(event)
    }

    @MessageAuthorize(text = ["农场等级"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun viewFarmLevel(event: GroupMessageEvent) {
        FarmUsecaseActions.level(event)
    }

    @MessageAuthorize(text = ["农场商店"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun viewShop(event: GroupMessageEvent) {
        FarmUsecaseActions.shop(event)
    }

    @MessageAuthorize(text = ["农场仓库"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun viewWarehouse(event: GroupMessageEvent) {
        FarmUsecaseActions.warehouse(event)
    }

    @MessageAuthorize(
        text = ["购买种子 \\S+( \\d+)?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun buySeed(event: GroupMessageEvent) {
        FarmUsecaseActions.buySeed(event)
    }

    @MessageAuthorize(
        text = ["(播种|种植) .+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun plant(event: GroupMessageEvent) {
        FarmUsecaseActions.plant(event)
    }

    @MessageAuthorize(
        text = ["收获 .+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun harvest(event: GroupMessageEvent) {
        FarmUsecaseActions.harvest(event)
    }

    @MessageAuthorize(
        text = ["卖出果实 .+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun sellFruits(event: GroupMessageEvent) {
        FarmUsecaseActions.sell(event)
    }

    @MessageAuthorize(
        text = ["升级农场\\*?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun upgradeFarm(event: GroupMessageEvent) {
        FarmUsecaseActions.upgrade(event)
    }

    @MessageAuthorize(
        text = ["帮浇水.*"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun water(event: GroupMessageEvent) {
        FarmUsecaseActions.water(event)
    }

    @MessageAuthorize(
        text = ["偷菜.*"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun steal(event: GroupMessageEvent) {
        FarmUsecaseActions.steal(event)
    }

    @MessageAuthorize(text = ["一键卖出"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun sellAll(event: GroupMessageEvent) {
        FarmUsecaseActions.sellAll(event)
    }

    @MessageAuthorize(text = ["一键收获"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun harvestAll(event: GroupMessageEvent) {
        FarmUsecaseActions.harvestAll(event)
    }

    @MessageAuthorize(
        text = ["一键播种 .+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        groupPermissions = [EconPerm.FARM_PERM]
    )
    suspend fun plantAll(event: GroupMessageEvent) {
        FarmUsecaseActions.plantAll(event)
    }

    @MessageAuthorize(text = ["激活守护"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun activateShield(event: GroupMessageEvent) {
        FarmUsecaseActions.shield(event)
    }

    @MessageAuthorize(text = ["黑市"], groupPermissions = [EconPerm.FARM_PERM])
    suspend fun blackMarket(event: GroupMessageEvent) {
        FarmUsecaseActions.blackMarket(event)
    }
}
