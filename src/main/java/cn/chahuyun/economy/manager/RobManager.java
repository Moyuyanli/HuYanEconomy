package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.AuthPerm;
import cn.chahuyun.authorize.constant.MessageConversionEnum;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.authorize.entity.PermGroup;
import cn.chahuyun.authorize.utils.PermUtil;
import cn.chahuyun.authorize.utils.UserUtil;
import cn.chahuyun.economy.constant.EconPerm;
import cn.chahuyun.economy.entity.UserFactor;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.UserStatus;
import cn.chahuyun.economy.plugin.FactorManager;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import cn.chahuyun.economy.utils.ShareUtils;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.val;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 抢劫管理
 *
 * @author Moyuyanli
 * @date 2022/11/15 10:01
 */
@EventComponent
public class RobManager {


    private final static Map<User, Date> cooling = new HashMap<>();


    @MessageAuthorize(
            text = "抢劫 ?@?\\d{6,11} ?",
            messageMatching = MessageMatchingEnum.REGULAR,
            messageConversion = MessageConversionEnum.CONTENT,
            groupPermissions = {EconPerm.ROB_PERM}
    )
    public void rob(GroupMessageEvent event) {
        Log.info("抢劫指令");

        Group group = event.getGroup();
        MessageChain message = event.getMessage();

        Member user = event.getSender();
        UserInfo userInfo = UserManager.getUserInfo(user);

        if (cooling.containsKey(user)) {
            Date date = cooling.get(user);
            long between = DateUtil.between(date, new Date(), DateUnit.MINUTE, true);
            if (between < 10) {
                group.sendMessage(MessageUtil.formatMessageChain(message, "再等等吧，你还得歇%d分钟才能行动!", 10 - between));
                return;
            }
        }

        cooling.put(user, new Date());

        if (UserStatusManager.checkUserNotInHome(userInfo)) {
            UserStatus userStatus = UserStatusManager.getUserStatus(user.getId());
            switch (userStatus.getPlace()) {
                //医院
                case HOSPITAL:
                    group.sendMessage(MessageUtil.formatMessageChain(message, "你还在医院嘞，你怎么抢劫？"));
                    return;
                //鱼塘
                case FISHPOND:
                    group.sendMessage(MessageUtil.formatMessageChain(message, "钓鱼了，不要分心！"));
                    return;
                //监狱
                case PRISON:
                    group.sendMessage(MessageUtil.formatMessageChain(message, "还在局子里面咧，不要幻想!"));
                    return;
                default:
                    group.sendMessage(MessageUtil.formatMessageChain(message, "你还没有准备好抢劫!"));
                    return;
            }
        }


        Member member = ShareUtils.getAtMember(event);

        if (member == null) {
            group.sendMessage(MessageUtil.formatMessageChain(message, "你抢劫的人不在这里!"));
            return;
        }

        UserInfo atUser = UserManager.getUserInfo(member);


        if (UserStatusManager.checkUserNotInHome(userInfo)) {
            UserStatus userStatus = UserStatusManager.getUserStatus(atUser);
            switch (userStatus.getPlace()) {
                case HOSPITAL:
                    group.sendMessage(MessageUtil.formatMessageChain(message, "医院禁止抢劫/打人！"));
                    return;
                case PRISON:
                    group.sendMessage(MessageUtil.formatMessageChain(message, "他还在局子里面，抢不到了！"));
                    return;
            }
        }


        UserFactor userFactor = FactorManager.getUserFactor(userInfo);
        UserFactor atUserFactor = FactorManager.getUserFactor(atUser);

        //抢劫因子
        int robFactor = ShareUtils.percentageToInt(FactorManager.getGlobalFactor().getRobFactor());

        int robRandom = RandomUtil.randomInt(0, 101);

        int force = ShareUtils.percentageToInt(userFactor.getForce());
        int atDoge = ShareUtils.percentageToInt(atUserFactor.getDodge());

        //抢劫成功
        if ((robRandom - force) <= (robFactor - atDoge)) {

            double atMoney = EconomyUtil.getMoneyByUser(member);

            //对方没钱
            if (atMoney < 1) {
                int hit = RandomUtil.randomInt(0, 101);

                int irritable = ShareUtils.percentageToInt(userFactor.getIrritable());

                //打他一顿
                if (hit <= irritable) {
                    group.sendMessage(MessageUtil.formatMessageChain(message, "你把他全身搜了个遍，连1块钱都拿不出来，你气不过，打了他一顿，把他打进医院了。"));

                    UserStatusManager.moveHospital(atUser, RandomUtil.randomInt(50, 501));
                } else {
                    group.sendMessage(MessageUtil.formatMessageChain(message, "他就穷光蛋一个，出门身上一块钱没有，真悲哀。"));
                }
                return;
            }

            int resistance = ShareUtils.percentageToInt(atUserFactor.getResistance());

            int resRandom = RandomUtil.randomInt(0, 101);
            double moneyRandom;
            if (resRandom <= resistance) {
                moneyRandom = ShareUtils.rounding(RandomUtil.randomDouble(0, atMoney / 2));
                group.sendMessage(MessageUtil.formatMessageChain(message, "对方拼命反抗，但是你还是抢到了对方%.1f的金币跑了...", moneyRandom));
            } else {
                moneyRandom = atMoney;
                group.sendMessage(MessageUtil.formatMessageChain(message, "这次抢劫很顺利，对方看起来弱小可怜又无助，你抢了他全部的%.1f金币", moneyRandom));
            }

            EconomyUtil.plusMoneyToUser(user, moneyRandom);
            EconomyUtil.minusMoneyToUser(member, moneyRandom);
            return;
        }

        int dodge = ShareUtils.percentageToInt(userFactor.getDodge());

        //进监狱
        if (robRandom - dodge > 75) {
            int quantity = 300;
            EconomyUtil.minusMoneyToUser(user, quantity);

            int recovery = 20;
            UserStatusManager.movePrison(userInfo, recovery);

            group.sendMessage(MessageUtil.formatMessageChain(message,
                    "你在抢劫的过程中，被警察发现了。%n" +
                            "被罚款%d金币，且拘留%d分钟", quantity, recovery));
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(message, "你正在实施抢劫，突然听见警笛，你头也不会的就跑了!"));
        }
    }


    @MessageAuthorize(
            text = "开启 抢劫",
            userPermissions = {AuthPerm.OWNER, AuthPerm.ADMIN}
    )
    public void startRob(GroupMessageEvent event) {
        Group group = event.getGroup();

        PermUtil util = PermUtil.INSTANCE;

        val user = UserUtil.INSTANCE.group(group.getId());

        if (util.checkUserHasPerm(user, EconPerm.ROB_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的抢劫已经开启了!"));
            return;
        }

        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.ROB_PERM_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的抢劫开启成功!"));
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的抢劫开启失败!"));
        }

    }

    @MessageAuthorize(
            text = "关闭 抢劫",
            userPermissions = {AuthPerm.OWNER, AuthPerm.ADMIN}
    )
    public void endRob(GroupMessageEvent event) {
        Group group = event.getGroup();

        PermUtil util = PermUtil.INSTANCE;

        val user = UserUtil.INSTANCE.group(group.getId());

        if (!util.checkUserHasPerm(user, EconPerm.ROB_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的抢劫已经关闭!"));
            return;
        }

        PermGroup permGroup = util.talkPermGroupByName(EconPerm.GROUP.ROB_PERM_GROUP);

        permGroup.getUsers().remove(user);
        permGroup.save();

        group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的抢劫关闭成功!"));
    }

}