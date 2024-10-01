package cn.chahuyun.economy.manager;

import cn.chahuyun.economy.constant.UserLocation;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.UserStatus;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * 用户状态管理
 *
 * @author Moyuyanli
 * @date 2024/9/26 9:48
 */
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
        UserStatus one = HibernateFactory.selectOne(UserStatus.class, qq);

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

}
