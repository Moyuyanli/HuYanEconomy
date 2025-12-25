package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.AuthPerm;
import cn.chahuyun.authorize.constant.MessageConversionEnum;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.economy.constant.UserLocation;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.UserStatus;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import cn.chahuyun.economy.utils.ShareUtils;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * 用户状态管理
 *
 * @author Moyuyanli
 * @date 2024/9/26 9:48
 */
@EventComponent
public class UserStatusManager {

    /**
     * 检查用户是否在家
     *
     * @param user 用户
     * @return true 在
     */
    public static boolean checkUserInHome(UserInfo user) {
        UserStatus userStatus = getUserStatus(user.getQq());

        return userStatus.getPlace() == UserLocation.HOME;
    }

    /**
     * 检查用户是否不在家
     *
     * @param user 用户
     * @return true 不在
     */
    public static boolean checkUserNotInHome(UserInfo user) {
        UserStatus userStatus = getUserStatus(user.getQq());

        return userStatus.getPlace() != UserLocation.HOME;
    }

    /**
     * 回家
     *
     * @param user 用户
     */
    public static void moveHome(UserInfo user) {
        UserStatus userStatus = getUserStatus(user.getQq());
        userStatus.setPlace(UserLocation.HOME);
        userStatus.setRecoveryTime(0);
        userStatus.setStartTime(new Date());
        HibernateFactory.merge(userStatus);
    }

    /**
     * 检查用户是否在医院
     *
     * @param user 用户
     * @return true 在
     */
    public static boolean checkUserInHospital(UserInfo user) {
        UserStatus userStatus = getUserStatus(user.getQq());

        return userStatus.getPlace() == UserLocation.HOSPITAL;
    }

    /**
     * 进医院咯~~<br>
     * 这里的复原时间是医疗费倍率。<br>
     * 具体计算方法是 2 * 倍率<br>
     * todo 在超过3天不付钱，再付医药费就是5*
     *
     * @param user     用户
     * @param recovery 医药费倍率(*分钟)
     */
    public static void moveHospital(UserInfo user, Integer recovery) {
        UserStatus userStatus = getUserStatus(user.getQq());
        userStatus.setPlace(UserLocation.HOSPITAL);
        userStatus.setRecoveryTime(recovery);
        userStatus.setStartTime(new Date());
        HibernateFactory.merge(userStatus);
    }

    /**
     * 检查用户是否在监狱
     *
     * @param user 用户
     * @return true 在
     */
    public static boolean checkUserInPrison(UserInfo user) {
        UserStatus userStatus = getUserStatus(user.getQq());

        return userStatus.getPlace() == UserLocation.PRISON;
    }

    /**
     * 蹲大牢咯~~
     *
     * @param user     用户
     * @param recovery 复原时间(分钟)
     */
    public static void movePrison(UserInfo user, Integer recovery) {
        UserStatus userStatus = getUserStatus(user.getQq());
        userStatus.setPlace(UserLocation.PRISON);
        userStatus.setRecoveryTime(recovery);
        userStatus.setStartTime(new Date());
        HibernateFactory.merge(userStatus);
    }

    /**
     * 检查用户是否在鱼塘
     *
     * @param user 用户
     * @return true 在
     */
    public static boolean checkUserInFishpond(UserInfo user) {
        UserStatus userStatus = getUserStatus(user.getQq());

        return userStatus.getPlace() == UserLocation.FISHPOND;
    }

    /**
     * 钓鱼去~~
     *
     * @param user     用户
     * @param recovery 复原时间(分钟)
     */
    public static void moveFishpond(UserInfo user, Integer recovery) {
        UserStatus userStatus = getUserStatus(user.getQq());
        userStatus.setPlace(UserLocation.FISHPOND);
        userStatus.setRecoveryTime(recovery);
        userStatus.setStartTime(new Date());
        HibernateFactory.merge(userStatus);
    }

    /**
     * 检查用户是否在工厂
     *
     * @param user 用户
     * @return true 在
     */
    public static boolean checkUserInFactory(UserInfo user) {
        UserStatus userStatus = getUserStatus(user.getQq());

        return userStatus.getPlace() == UserLocation.HOME;
    }

    /**
     * 进厂子~~
     *
     * @param user     用户
     * @param recovery 复原时间(分钟)
     */
    public static void moveFactory(UserInfo user, Integer recovery) {
        UserStatus userStatus = getUserStatus(user.getQq());
        userStatus.setPlace(UserLocation.FACTORY);
        userStatus.setRecoveryTime(recovery);
        userStatus.setStartTime(new Date());
        HibernateFactory.merge(userStatus);
    }

    /**
     * 获取用户状态
     *
     * @param user 用户信息
     * @return 用户状态
     */
    @NotNull
    public static UserStatus getUserStatus(@NotNull UserInfo user) {
        return getUserStatus(user.getQq());
    }

    /**
     * 获取用户状态
     *
     * @param qq 用户qq
     * @return 用户状态
     */
    @NotNull
    public static UserStatus getUserStatus(@NotNull Long qq) {
        UserStatus one = HibernateFactory.selectOneById(UserStatus.class, qq);

        if (one == null) {
            UserStatus status = new UserStatus();
            status.setId(qq);
            return HibernateFactory.merge(status);
        }

        //复原时间检测
        Integer time = one.getRecoveryTime();
        if (time != 0 && one.getPlace() != UserLocation.HOSPITAL) {
            Date startTime = one.getStartTime();
            long between = DateUtil.between(startTime, new Date(), DateUnit.MINUTE, true);
            if (between > time) {
                one.setRecoveryTime(0);
                one.setPlace(UserLocation.HOME);
                return HibernateFactory.merge(one);
            }
        }

        return one;
    }

    @MessageAuthorize(text = {"我的状态", "我的位置"})
    public void myStatus(GroupMessageEvent event) {
        Member sender = event.getSender();
        MessageChain message = event.getMessage();
        Group group = event.getGroup();

        UserInfo userInfo = UserManager.getUserInfo(sender);

        UserStatus userStatus = getUserStatus(userInfo);

        UserLocation place = userStatus.getPlace();
        switch (place) {
            case HOME:
                group.sendMessage(MessageUtil.formatMessageChain(message, "你现在正在家里躺着哩~"));
                return;
            case PRISON:
                Integer time = userStatus.getRecoveryTime();
                long between = DateUtil.between(userStatus.getStartTime(), new Date(), DateUnit.MINUTE);
                group.sendMessage(MessageUtil.formatMessageChain(message, "你还在监狱，剩余拘禁时间:%s分钟", time - between));
                return;
            case HOSPITAL:
                group.sendMessage(MessageUtil.formatMessageChain(message, "你现在正在医院躺着，wifi速度还行."));
                return;
            case FACTORY:
                group.sendMessage(MessageUtil.formatMessageChain(message, "你现在正在工厂，这里很吵闹!"));
                return;
            case FISHPOND:
                group.sendMessage(MessageUtil.formatMessageChain(message, "嘘，别把我的鱼吓跑了!"));
                return;
        }
    }


//    public static boolean checkUserInHome(UserInfo user) {
//        UserStatus userStatus = getUserStatus(user.getQq());
//
//        return userStatus.getPlace() == UserLocation.HOME;
//    }
//    public static boolean checkUserInHome(User user) {
//        UserStatus userStatus = getUserStatus(user.getId());
//
//        return userStatus.getPlace() == UserLocation.HOME;
//    }

    @MessageAuthorize(text = "回家")
    public void goHome(GroupMessageEvent event) {
        Group group = event.getGroup();
        MessageChain message = event.getMessage();
        Member sender = event.getSender();

        UserInfo userInfo = UserManager.getUserInfo(sender);

        if (checkUserInHome(userInfo)) {
            group.sendMessage(MessageUtil.formatMessageChain(message, "你已经在家里了！"));
            return;
        }

        if (checkUserInHospital(userInfo) || checkUserInPrison(userInfo)) {
            group.sendMessage(MessageUtil.formatMessageChain(message, "你现在还不能回家！"));
        } else {
            moveHome(userInfo);
            group.sendMessage(MessageUtil.formatMessageChain(message, "你回家躺着去."));
        }
    }

    @MessageAuthorize(text = "回家 ?@\\d+",
            messageMatching = MessageMatchingEnum.REGULAR,
            messageConversion = MessageConversionEnum.CONTENT,
            userPermissions = {AuthPerm.OWNER, AuthPerm.ADMIN}
    )
    public void gotoHome(GroupMessageEvent event) {
        Group group = event.getGroup();
        MessageChain message = event.getMessage();

        Member member = ShareUtils.getAtMember(event);

        UserInfo userInfo;
        if (member == null) {
            Log.warning("该用户不存在!");
            return;
        }
        userInfo = UserManager.getUserInfo(member);

        moveHome(userInfo);
        group.sendMessage(MessageUtil.formatMessageChain(message, "你让ta回家躺着去."));
    }

}
