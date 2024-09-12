package cn.chahuyun.economy.plugin;

import cn.chahuyun.authorize.PermissionServer;
import cn.chahuyun.authorize.entity.Perm;
import cn.chahuyun.authorize.entity.PermGroup;
import cn.chahuyun.authorize.utils.PermUtil;
import cn.chahuyun.economy.constant.EconPerm;
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin;

/**
 * @author Moyuyanli
 * @date 2024/9/2 14:47
 */
public class PermCodeManager {

    private PermCodeManager() {
    }


    public static void init(JvmPlugin plugin) {
        PermissionServer instance = PermissionServer.INSTANCE;

        instance.registerPermCode(plugin,
                new Perm(EconPerm.FISH_PERM, "壶言经济的钓鱼权限"),
                new Perm(EconPerm.LOTTERY_PERM, "壶言经济的彩票权限"),
                new Perm(EconPerm.ROB_PERM, "壶言经济的抢劫权限"),
                new Perm(EconPerm.RED_PACKET_PERM, "壶言经济的红包权限"),
                new Perm(EconPerm.SIGN_BLACK_GROUP, "壶言经济的签到黑名单权限")
        );

        PermUtil util = PermUtil.INSTANCE;

        PermGroup group = util.selectPermGroupOneByName(EconPerm.FISH_PERM_GROUP);

        if (group == null) {
            group = util.talkPermGroupByName(EconPerm.FISH_PERM_GROUP);
            Perm perm = util.takePerm(EconPerm.FISH_PERM);
            util.addPermToPermGroupByPermGroup(perm, group);
        }

        group = util.selectPermGroupOneByName(EconPerm.ROB_PERM_GROUP);

        if (group == null) {
            group = util.talkPermGroupByName(EconPerm.ROB_PERM_GROUP);
            Perm perm = util.takePerm(EconPerm.ROB_PERM);
            util.addPermToPermGroupByPermGroup(perm, group);
        }

        group = util.selectPermGroupOneByName(EconPerm.LOTTERY_PERM_GROUP);

        if (group == null) {
            group = util.talkPermGroupByName(EconPerm.LOTTERY_PERM_GROUP);
            Perm perm = util.takePerm(EconPerm.LOTTERY_PERM);
            util.addPermToPermGroupByPermGroup(perm, group);
        }

        group = util.selectPermGroupOneByName(EconPerm.RED_PACKET_PERM_GROUP);

        if (group == null) {
            group = util.talkPermGroupByName(EconPerm.RED_PACKET_PERM_GROUP);
            Perm perm = util.takePerm(EconPerm.RED_PACKET_PERM);
            util.addPermToPermGroupByPermGroup(perm, group);
        }

        group = util.selectPermGroupOneByName(EconPerm.SIGN_BLACK_GROUP);

        if (group == null) {
            group = util.talkPermGroupByName(EconPerm.SIGN_BLACK_GROUP);
            Perm perm = util.takePerm(EconPerm.SIGN_BLACK_PERM);
            util.addPermToPermGroupByPermGroup(perm, group);
        }

    }

}
