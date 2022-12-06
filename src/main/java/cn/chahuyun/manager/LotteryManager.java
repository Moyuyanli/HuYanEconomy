package cn.chahuyun.manager;

import cn.chahuyun.HuYanEconomy;
import cn.chahuyun.config.ConfigData;
import cn.chahuyun.entity.LotteryInfo;
import cn.chahuyun.util.HibernateUtil;
import cn.chahuyun.util.Log;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 彩票管理<p>
 * 3种彩票：<p>
 * 一分钟开一次<p>
 * 一小时开一次<p>
 * 一天开一次<p>
 * 160 6 0.7 <p>
 * 1250 35 2.5 0.5<p>
 * 10000 200 12 1.4 0.3<p>
 *
 * @author Moyuyanli
 * @date 2022/11/15 10:01
 */
public class LotteryManager {


    private LotteryManager() {
    }

    private static final Map<String, LotteryInfo> minutesLottery = new HashMap<>();
    private static final Map<String, LotteryInfo> hoursLottery = new HashMap<>();
    private static final Map<String, LotteryInfo> dayLottery = new HashMap<>();


    /**
     * 初始化彩票<p>
     *
     * @author Moyuyanli
     * @date 2022/12/6 11:23
     */
    public static void init(boolean type) {
        List<LotteryInfo> lotteryInfos;
        try {
            lotteryInfos = HibernateUtil.factory.fromSession(session -> {
                HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
                JpaCriteriaQuery<LotteryInfo> query = builder.createQuery(LotteryInfo.class);
                JpaRoot<LotteryInfo> from = query.from(LotteryInfo.class);
                query.select(from);
                return session.createQuery(query).list();
            });
        } catch (Exception e) {
            Log.error("彩票管理:彩票初始化失败!", e);
            return;
        }


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
                    continue;
            }
        }

        if (minutesLottery.size() > 0) {
            LotteryMinutesTask minutesTask = new LotteryMinutesTask("minutesTask", minutesLottery.values());
            CronUtil.schedule("minutesTask", "0 * * * * ?", minutesTask);
        }
        if (hoursLottery.size() > 0) {
            LotteryHoursTask hoursTask = new LotteryHoursTask("hoursTask", hoursLottery.values());
            CronUtil.schedule("hoursTask", "0 0 * * * ?", hoursTask);
        }
        if (dayLottery.size() > 0) {
            var dayTask = new LotteryDayTask("dayTask", dayLottery.values());
            CronUtil.schedule("dayTask", "0 0 0 * * ?", dayTask);
        }
        if (type) {
            CronUtil.start();
        }
    }

    /**
     * 购买一个彩票<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/6 11:23
     */
    public static void addLottery(MessageEvent event) {
        User user = event.getSender();
        Contact subject = event.getSubject();

        if (subject instanceof Group) {
            Group group = (Group) subject;
            List<Long> longs = ConfigData.INSTANCE.getGroup();
            if (!longs.contains(group.getId())) {
                return;
            }
        }


        MessageChain message = event.getMessage();
        String code = message.serializeToMiraiCode();

        String[] split = code.split(" ");
        StringBuilder number = new StringBuilder(split[1]);

        double money = Double.parseDouble(split[2]);


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
                subject.sendMessage("猜签信息错误!");
                return;
        }
        String string = number.toString();
        char[] chars = string.toCharArray();
        number = new StringBuilder(String.valueOf(chars[0]));
        for (int i = 1; i < string.length(); i++) {
            String aByte = String.valueOf(chars[i]);
            number.append(",").append(aByte);
        }
        LotteryInfo lotteryInfo = new LotteryInfo(user.getId(), subject.getId(), money, type, string);
        lotteryInfo.save();
        subject.sendMessage(String.format("猜签成功:\n猜签类型:%s\n猜签号码:%s\n猜签金额:%s", typeString, number, money));
        init(false);
    }

    /**
     * 发送彩票结果信息
     *
     * @param type 彩票类型
     * @param location 猜中数量
     * @param lotteryInfo 彩票信息
     * @author Moyuyanli
     * @date 2022/12/6 16:52
     */
    public static void result(int type, int location, LotteryInfo lotteryInfo) {
        Bot bot = HuYanEconomy.bot;
        if (type == 1) {
            Group group = bot.getGroup(lotteryInfo.getGroup());
            assert group != null;
            NormalMember member = group.get(lotteryInfo.getQq());
            assert member != null;
            member.sendMessage(lotteryInfo.toMessage());
            if (location == 3) {
                group.sendMessage(String.format("得签着:%s(%s),奖励%s金币", member.getNick(), member.getId(),lotteryInfo.getBonus()));
            }
            lotteryInfo.remove();
            minutesLottery.remove(lotteryInfo);
        }
    }

}


/**
 * 彩票定时任务<p>
 * 分钟<p>
 *
 * @author Moyuyanli
 * @date 2022/12/6 14:43
 */
class LotteryMinutesTask implements Task {

    private String id;
    private List<LotteryInfo> lotteryInfos;

    LotteryMinutesTask(String id, Collection<LotteryInfo> lotteryInfos) {
        this.id = id;
        this.lotteryInfos = List.copyOf(lotteryInfos);
    }

    /**
     * 执行作业
     * <p>
     * 作业的具体实现需考虑异常情况，默认情况下任务异常在监听中统一监听处理，如果不加入监听，异常会被忽略<br>
     * 因此最好自行捕获异常后处理
     */
    @Override
    public void execute() {
        Bot bot = HuYanEconomy.bot;

        int random1 = RandomUtil.randomInt(0, 9);
        int random2 = RandomUtil.randomInt(0, 9);
        int random3 = RandomUtil.randomInt(0, 9);
        String[] current = {String.valueOf(random1), String.valueOf(random2), String.valueOf(random3)};
        StringBuilder currentString = new StringBuilder(current[0]);
        for (int i = 1; i < current.length; i++) {
            String s = current[i];
            currentString.append(",").append(s);
        }


        Set<Long> groups = new HashSet<>();

        for (LotteryInfo lotteryInfo : lotteryInfos) {
            groups.add(lotteryInfo.getGroup());
            //位置正确的数量
            int location = 0;
            //计算奖金
            double bonus = 0;

            String[] split = lotteryInfo.getNumber().split(",");
            for (int i = 0; i < split.length; i++) {
                String s = split[i];
                if (s.equals(current[i])) {
                    location++;
                }
            }
            switch (location) {
                case 3:
                    bonus = lotteryInfo.getMoney() * 160;
                    break;
                case 2:
                    bonus = lotteryInfo.getMoney() * 6;
                    break;
                case 1:
                    bonus = lotteryInfo.getMoney() * 0.7;
                    break;
            }
            lotteryInfo.setBonus(bonus);
            lotteryInfo.setCurrent(currentString.toString());
            lotteryInfo.save();
            LotteryManager.result(1, location, lotteryInfo);
        }
        for (Long group : groups) {
            String format = String.format("本期小签开签啦！\n开签号码%s", currentString);
            Objects.requireNonNull(bot.getGroup(group)).sendMessage(format);
        }
        CronUtil.remove(id);
    }
}

/**
 * 彩票定时任务<p>
 * 小时<p>
 *
 * @author Moyuyanli
 * @date 2022/12/6 14:43
 */
class LotteryHoursTask implements Task {
    private String id;
    private List<LotteryInfo> lotteryInfos;

    LotteryHoursTask(String id, Collection<LotteryInfo> lotteryInfos) {
        this.id = id;
        this.lotteryInfos = List.copyOf(lotteryInfos);
    }

    /**
     * 执行作业
     * <p>
     * 作业的具体实现需考虑异常情况，默认情况下任务异常在监听中统一监听处理，如果不加入监听，异常会被忽略<br>
     * 因此最好自行捕获异常后处理
     */
    @Override
    public void execute() {

    }
}

/**
 * 彩票定时任务<p>
 * 天<p>
 *
 * @author Moyuyanli
 * @date 2022/12/6 14:43
 */
class LotteryDayTask implements Task {

    private String id;
    private List<LotteryInfo> lotteryInfos;

    LotteryDayTask(String id, Collection<LotteryInfo> lotteryInfos) {
        this.id = id;
        this.lotteryInfos = List.copyOf(lotteryInfos);
    }

    /**
     * 执行作业
     * <p>
     * 作业的具体实现需考虑异常情况，默认情况下任务异常在监听中统一监听处理，如果不加入监听，异常会被忽略<br>
     * 因此最好自行捕获异常后处理
     */
    @Override
    public void execute() {

    }
}