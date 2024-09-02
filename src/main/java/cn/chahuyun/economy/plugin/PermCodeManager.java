package cn.chahuyun.economy.plugin;

import cn.chahuyun.authorize.PermissionServer;
import cn.chahuyun.authorize.entity.Perm;
import cn.chahuyun.authorize.entity.PermGroup;
import cn.chahuyun.authorize.entity.User;
import cn.chahuyun.authorize.utils.PermUtil;
import cn.chahuyun.economy.constant.PermCode;
import cn.chahuyun.hibernateplus.HibernateFactory;
import lombok.val;
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

        val group = util.selectOneByName(PermCode.FISH_PERM_GROUP);

        if (group == null) {
            PermGroup permGroup = new PermGroup(PermCode.FISH_PERM_GROUP, null);
            Perm perm = HibernateFactory.selectOne(Perm.class, "code", PermCode.FISH_PERM);
            permGroup.getPerms().add(perm);

            HibernateFactory.merge(permGroup);
        }


        User.Companion

    }

}
