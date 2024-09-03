package cn.chahuyun.economy.plugin;

import cn.chahuyun.authorize.PermissionServer;
import cn.chahuyun.authorize.entity.Perm;
import cn.chahuyun.authorize.entity.PermGroup;
import cn.chahuyun.authorize.utils.PermUtil;
import cn.chahuyun.economy.constant.PermCode;
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
                new Perm(PermCode.FISH_PERM, "壶言经济的钓鱼权限"),
                new Perm(PermCode.LOTTERY_PERM, "壶言经济的彩票权限"),
                new Perm(PermCode.ROB_PERM, "壶言经济的抢劫权限")
        );

        PermUtil util = PermUtil.INSTANCE;

        PermGroup group = util.selectPermGroupOneByName(PermCode.FISH_PERM_GROUP);

        if (group == null) {
            group = util.talkPermGroupByName(PermCode.FISH_PERM_GROUP);
            Perm perm = util.takePerm(PermCode.FISH_PERM);
            util.addPermToPermGroupByPermGroup(perm, group);
        }

        group = util.selectPermGroupOneByName(PermCode.ROB_PERM_GROUP);

        if (group == null) {
            group = util.talkPermGroupByName(PermCode.ROB_PERM_GROUP);
            Perm perm = util.takePerm(PermCode.ROB_PERM);
            util.addPermToPermGroupByPermGroup(perm, group);
        }

        group = util.selectPermGroupOneByName(PermCode.LOTTERY_PERM_GROUP);

        if (group == null) {
            group = util.talkPermGroupByName(PermCode.LOTTERY_PERM_GROUP);
            Perm perm = util.takePerm(PermCode.LOTTERY_PERM);
            util.addPermToPermGroupByPermGroup(perm, group);
        }

    }

}
