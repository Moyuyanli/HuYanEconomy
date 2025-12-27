package cn.chahuyun.economy.action;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.AuthPerm;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.authorize.entity.PermGroup;
import cn.chahuyun.authorize.utils.PermUtil;
import cn.chahuyun.authorize.utils.UserUtil;
import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.constant.EconPerm;
import cn.chahuyun.economy.entity.LotteryInfo;
import cn.chahuyun.economy.manager.LotteryDayTask;
import cn.chahuyun.economy.manager.LotteryHoursTask;
import cn.chahuyun.economy.manager.LotteryMinutesTask;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.cron.CronUtil;
import lombok.val;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 彩票管理<p>
 * 3种彩票：<p>
 * 一分钟开一次<p>
 * 一小时开一次<p>
 * 一天开一次<p>
 *
 * @author Moyuyanli
 * @date 2022/11/15 10:01
 */
@EventComponent
public class LotteryAction {

    public static final AtomicBoolean minuteTiming = new AtomicBoolean(false);
    public static final AtomicBoolean hoursTiming = new AtomicBoolean(false);


    /**
     * 初始化彩票<p>
     *
     * @author Moyuyanli
     * @date 2022/12/6 11:23
     */
    public static void init() {
        List<LotteryInfo> lotteryInfos;
        try {
            lotteryInfos = HibernateFactory.selectList(LotteryInfo.class);
        } catch (Exception e) {
            Log.error("彩票管理:彩票初始化失败!", e);
            return;
        }


        Map<String, LotteryInfo> minutesLottery = new HashMap<>();
        Map<String, LotteryInfo> hoursLottery = new HashMap<>();
        Map<String, LotteryInfo> dayLottery = new HashMap<>();

        for (LotteryInfo lotteryInfo : lotteryInfos) {
            switch (lotteryInfo.getType()) {
                case 1:
                    minutesLottery.put(lotteryInfo.getNumber(), lotteryInfo);
                    continue;
                case 2:
                    hoursLottery.put(lotteryInfo.getNumber(), lotteryInfo);
                    continue;
                case 3:
                    dayLottery.put(lotteryInfo.getNumber(), lotteryInfo);
            }
        }

        if (!minutesLottery.isEmpty()) {
            extractedMinutes();
        }

        if (!hoursLottery.isEmpty()) {
            extractedHours();
        }

        if (!dayLottery.isEmpty()) {
            String dayTaskId = "dayTask";
            CronUtil.remove(dayTaskId);
            var dayTask = new LotteryDayTask(dayTaskId);
            CronUtil.schedule(dayTaskId, "0 0 0 * * ?", dayTask);
        }
    }

    private static void extractedHours() {
        if (hoursTiming.get()) {
            return;
        }
        String hoursTaskId = "hoursTask";
        CronUtil.remove(hoursTaskId);
        LotteryHoursTask hoursTask = new LotteryHoursTask(hoursTaskId);
        CronUtil.schedule(hoursTaskId, "0 0 * * * ?", hoursTask);
        hoursTiming.set(true);
    }

    private static void extractedMinutes() {
        if (minuteTiming.get()) {
            return;
        }
        //唯一id
        String minutesTaskId = "minutesTask";
        //始终删除一次  用于防止刷新的时候 添加定时任务报错
        CronUtil.remove(minutesTaskId);
        //建立任务类
        LotteryMinutesTask minutesTask = new LotteryMinutesTask(minutesTaskId);
        //添加定时任务到调度器
        CronUtil.schedule(minutesTaskId, "0 * * * * ?", minutesTask);
        minuteTiming.set(true);
    }

    /**
     * 购买一个彩票<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/6 11:23
     */
    @MessageAuthorize(
            text = "猜签 (\\d+)( \\d+)|lottery (\\d+)( \\d+)",
            messageMatching = MessageMatchingEnum.REGULAR,
            groupPermissions = EconPerm.LOTTERY_PERM
    )
    public void addLottery(GroupMessageEvent event) {
        Log.info("彩票指令");

        User user = event.getSender();
        Contact subject = event.getSubject();

        MessageChain message = event.getMessage();
        String code = message.serializeToMiraiCode();

        String[] split = code.split(" ");
        StringBuilder number = new StringBuilder(split[1]);

        double money = Double.parseDouble(split[2]);

        double moneyByUser = EconomyUtil.getMoneyByUser(user);
        if (moneyByUser - money <= 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你都穷的叮当响了，还来猜签？"));
            return;
        }

        int type;
        String typeString;
        switch (number.length()) {
            case 3:
                type = 1;
                typeString = "小签";
                break;
            case 4:
                type = 2;
                typeString = "中签";
                break;
            case 5:
                type = 3;
                typeString = "大签";
                break;
            default:
                subject.sendMessage(MessageUtil.formatMessageChain(message, "猜签类型错误!"));
                return;
        }

        if (type == 1) {
            if (!(0 < money && money <= 1000)) {
                subject.sendMessage(MessageUtil.formatMessageChain(message, "你投注的金额不属于这个签!"));
                return;
            }
        } else if (type == 2) {
            if (!(0 < money && money <= 10000)) {
                subject.sendMessage(MessageUtil.formatMessageChain(message, "你投注的金额不属于这个签!"));
                return;
            }
        } else {
            if (!(0 < money && money <= 1000000)) {
                subject.sendMessage(MessageUtil.formatMessageChain(message, "你投注的金额不属于这个签!"));
                return;
            }
        }

        String string = number.toString();
        char[] chars = string.toCharArray();
        number = new StringBuilder(String.valueOf(chars[0]));
        for (int i = 1; i < string.length(); i++) {
            String aByte = String.valueOf(chars[i]);
            number.append(",").append(aByte);
        }
        LotteryInfo lotteryInfo = new LotteryInfo(user.getId(), subject.getId(), money, type, number.toString());
        if (!EconomyUtil.minusMoneyToUser(user, money)) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "猜签失败！"));
            return;
        }
        HibernateFactory.merge(lotteryInfo);
        subject.sendMessage(MessageUtil.formatMessageChain(message, "猜签成功:\n猜签类型:%s\n猜签号码:%s\n猜签金币:%s", typeString, number, money));

        if (type == 1) {
            extractedMinutes();
        } else if (type == 2) {
            extractedHours();
        }
    }

    /**
     * 发送彩票结果信息
     * <p>
     *
     * @param type        彩票类型
     * @param location    猜中数量
     * @param lotteryInfo 彩票信息
     * @author Moyuyanli
     * @date 2022/12/6 16:52
     */
    public static void result(int type, int location, LotteryInfo lotteryInfo) {
        if (location == 0) {
            // 我找你半年了，原来问题出在这里，艹!
            lotteryInfo.remove();
            return;
        }
        Bot bot = HuYanEconomy.INSTANCE.bot;
        Group group = bot.getGroup(lotteryInfo.getGroup());
        assert group != null;
        NormalMember member = group.get(lotteryInfo.getQq());
        assert member != null;
        lotteryInfo.remove();

        if (!EconomyUtil.plusMoneyToUser(member, lotteryInfo.getBonus())) {
            member.sendMessage("奖金添加失败，请联系管理员!");
            return;
        }

        member.sendMessage(lotteryInfo.toMessage());
        switch (type) {
            case 1:
                if (location == 3) {
                    group.sendMessage(String.format("得签着:%s(%s),奖励%s金币", member.getNick(), member.getId(), lotteryInfo.getBonus()));
                }
                break;
            case 2:
                if (location == 4) {
                    group.sendMessage(String.format("得签着:%s(%s),奖励%s金币", member.getNick(), member.getId(), lotteryInfo.getBonus()));
                }
                break;
            case 3:
                if (location == 5) {
                    group.sendMessage(String.format("得签着:%s(%s),奖励%s金币", member.getNick(), member.getId(), lotteryInfo.getBonus()));
                }
                break;
        }
    }

    /**
     * 关闭定时器
     */
    public static void close() {
        CronUtil.stop();
    }


    @MessageAuthorize(
            text = "开启 猜签",
            userPermissions = {AuthPerm.OWNER, AuthPerm.ADMIN}
    )
    public void startLottery(GroupMessageEvent event) {
        Group group = event.getGroup();

        PermUtil util = PermUtil.INSTANCE;

        val user = UserUtil.INSTANCE.group(group.getId());

        if (util.checkUserHasPerm(user, EconPerm.LOTTERY_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的猜签已经开启了!"));
            return;
        }

        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.LOTTERY_PERM_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的猜签开启成功!"));
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的猜签开启失败!"));
        }

    }

    @MessageAuthorize(
            text = "关闭 猜签",
            userPermissions = {AuthPerm.OWNER, AuthPerm.ADMIN}
    )
    public void endLottery(GroupMessageEvent event) {
        Group group = event.getGroup();

        PermUtil util = PermUtil.INSTANCE;

        val user = UserUtil.INSTANCE.group(group.getId());

        if (!util.checkUserHasPerm(user, EconPerm.LOTTERY_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的猜签已经关闭!"));
            return;
        }

        PermGroup permGroup = util.takePermGroupByName(EconPerm.GROUP.LOTTERY_PERM_GROUP);

        permGroup.getUsers().remove(user);
        permGroup.save();

        group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的猜签关闭成功!"));
    }

}