package cn.chahuyun.economy.plugin

import cn.chahuyun.authorize.AuthorizeServer
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.economy.constant.EconPerm
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin

object PermCodeManager {
    @JvmStatic
    fun init(plugin: JvmPlugin) {

        AuthorizeServer.registerPermissions(
            plugin,
            Perm(EconPerm.FISH_PERM, "壶言经济的钓鱼权限"),
            Perm(EconPerm.LOTTERY_PERM, "壶言经济的彩票权限"),
            Perm(EconPerm.ROB_PERM, "壶言经济的抢劫权限"),
            Perm(EconPerm.RED_PACKET_PERM, "壶言经济的红包权限"),
            Perm(EconPerm.SIGN_BLACK_PERM, "壶言经济的签到黑名单权限"),
            Perm(EconPerm.RAFFLE_PERM, "壶言经济的抽奖权限")
        )

        val util = PermUtil
        var group: PermGroup? = util.selectPermGroupOneByName(EconPerm.GROUP.FISH_PERM_GROUP)
        if (group == null) {
            group = util.takePermGroupByName(EconPerm.GROUP.FISH_PERM_GROUP)
            val perm = util.takePerm(EconPerm.FISH_PERM)
            util.addPermToPermGroupByPermGroup(perm, group)
        }

        group = util.selectPermGroupOneByName(EconPerm.GROUP.ROB_PERM_GROUP)
        if (group == null) {
            group = util.takePermGroupByName(EconPerm.GROUP.ROB_PERM_GROUP)
            val perm = util.takePerm(EconPerm.ROB_PERM)
            util.addPermToPermGroupByPermGroup(perm, group)
        }

        group = util.selectPermGroupOneByName(EconPerm.GROUP.LOTTERY_PERM_GROUP)
        if (group == null) {
            group = util.takePermGroupByName(EconPerm.GROUP.LOTTERY_PERM_GROUP)
            val perm = util.takePerm(EconPerm.LOTTERY_PERM)
            util.addPermToPermGroupByPermGroup(perm, group)
        }

        group = util.selectPermGroupOneByName(EconPerm.GROUP.RED_PACKET_PERM_GROUP)
        if (group == null) {
            group = util.takePermGroupByName(EconPerm.GROUP.RED_PACKET_PERM_GROUP)
            val perm = util.takePerm(EconPerm.RED_PACKET_PERM)
            util.addPermToPermGroupByPermGroup(perm, group)
        }

        group = util.selectPermGroupOneByName(EconPerm.GROUP.SIGN_BLACK_GROUP)
        if (group == null) {
            group = util.takePermGroupByName(EconPerm.GROUP.SIGN_BLACK_GROUP)
            val perm = util.takePerm(EconPerm.SIGN_BLACK_PERM)
            util.addPermToPermGroupByPermGroup(perm, group)
        }

        group = util.selectPermGroupOneByName(EconPerm.GROUP.RAFFLE_PERM_GROUP)
        if (group == null) {
            group = util.takePermGroupByName(EconPerm.GROUP.RAFFLE_PERM_GROUP)
            val perm = util.takePerm(EconPerm.RAFFLE_PERM)
            util.addPermToPermGroupByPermGroup(perm, group)
        }
    }
}
