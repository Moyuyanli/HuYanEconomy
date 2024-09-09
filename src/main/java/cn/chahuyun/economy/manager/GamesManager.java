package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.PermConstant;
import cn.chahuyun.authorize.entity.PermGroup;
import cn.chahuyun.authorize.utils.PermUtil;
import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.constant.FishPondLevelConstant;
import cn.chahuyun.economy.constant.PermCode;
import cn.chahuyun.economy.constant.TitleCode;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.fish.Fish;
import cn.chahuyun.economy.entity.fish.FishInfo;
import cn.chahuyun.economy.entity.fish.FishPond;
import cn.chahuyun.economy.entity.fish.FishRanking;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import cn.chahuyun.economy.utils.ShareUtils;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import lombok.val;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.chahuyun.economy.HuYanEconomy.msgConfig;

/**
 * 游戏管理<p>
 * 24点|钓鱼<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:26
 */
@EventComponent
public class GamesManager {

    /**
     * 玩家钓鱼冷却
     */
    private static final Map<Long, Date> playerCooling = new HashMap<>();


    public static void init() {
        Task task = new Task() {

            /**
             * 执行作业
             * <p>
             * 作业的具体实现需考虑异常情况，默认情况下任务异常在监听中统一监听处理，如果不加入监听，异常会被忽略<br>
             * 因此最好自行捕获异常后处理
             */
            @Override
            public void execute() {
                List<FishPond> pondType = HibernateFactory.selectList(FishPond.class, "pondType", 1);
                for (FishPond fishPond : pondType) {
                    double fishPondMoney = fishPond.getFishPondMoney();

                    int level = fishPond.getPondLevel() - 1;
                    FishPondLevelConstant value = FishPondLevelConstant.values()[level];

                    if (fishPondMoney >= value.getAmount()) {
                        if (EconomyUtil.plusMoneyToPluginBankForId(fishPond.getCode(), fishPond.getDescription(), -30000)) {
                            Bot bot = HuYanEconomy.INSTANCE.bot;
                            Group group = bot.getGroup(fishPond.getId());
                            if (group != null) {
                                group.sendMessage(String.format(
                                        "鱼塘 %s 已经积攒够了升级的资金！开始升级鱼塘了！%n" +
                                                "鱼塘等级:%d -> %d%n" +
                                                "最低鱼竿等级:%d", fishPond.getName(), level + 1, level + 2, value.getMinFishLevel()
                                ));
                            } else {
                                Objects.requireNonNull(bot.getFriend(fishPond.getAdmin())).sendMessage("群鱼塘升级了");
                            }
                            fishPond.setPondLevel(level + 2);
                            fishPond.setMinLevel(value.getMinFishLevel());
                            fishPond.save();
                        }
                    }
                }
            }
        };

        CronUtil.schedule("0 0 12 * * ?", task);
    }

    /**
     * 开始钓鱼游戏
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/9 16:16
     */
    @MessageAuthorize(
            text = {"钓鱼", "抛竿"},
            groupPermissions = PermCode.FISH_PERM
    )
    public static void fishing(GroupMessageEvent event) {
        Log.info("钓鱼指令");

        UserInfo userInfo = UserManager.getUserInfo(event.getSender());
        User user = userInfo.getUser();
        Group subject = event.getSubject();
        //获取玩家钓鱼信息
        FishInfo fishInfo = userInfo.getFishInfo();
        //能否钓鱼
        if (fishInfo == null || !fishInfo.isFishRod()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), msgConfig.getNoneRodMsg()));
            return;
        }
        //是否已经在钓鱼
        if (fishInfo.getStatus()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), msgConfig.getFishingNowMsg()));
            return;
        }
        //钓鱼佬称号buff
        boolean isFishing = TitleManager.checkTitleIsExist(userInfo, TitleCode.FISHING);
        //钓鱼冷却
        if (playerCooling.containsKey(userInfo.getQq())) {
            Date date = playerCooling.get(userInfo.getQq());
            long between = DateUtil.between(date, new Date(), DateUnit.MINUTE, true);
            int expired = 10;
            if (isFishing) {
                expired = 3;
            } else {
                expired = ((expired * 60) - (fishInfo.getRodLevel() * 3)) / 60;
            }
            if (between < expired) {
                subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "你还差%s分钟来抛第二杆!", expired - between));
                return;
            } else {
                playerCooling.remove(userInfo.getQq());
            }
        } else {
            playerCooling.put(userInfo.getQq(), new Date());
        }
        //是否已经在钓鱼
        if (fishInfo.isStatus()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), msgConfig.getFishingNowMsg()));
            return;
        }
        //获取鱼塘
        FishPond fishPond = fishInfo.getFishPond(subject);
        if (fishPond == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "默认鱼塘不存在!"));
            return;
        }
        //获取鱼塘限制鱼竿最低等级
        int minLevel = fishPond.getMinLevel();
        if (fishInfo.getRodLevel() < minLevel) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), msgConfig.getRodLevelNotEnough()));
            return;
        }
        //开始钓鱼
        String start = String.format("%s开始钓鱼\n鱼塘:%s\n等级:%s\n最低鱼竿等级:%s\n%s", userInfo.getName(), fishPond.getName(), fishPond.getPondLevel(), fishPond.getMinLevel(), fishPond.getDescription());
        subject.sendMessage(start);
        Log.info(String.format("%s开始钓鱼", userInfo.getName()));

        //初始钓鱼信息
        boolean theRod = false;
        int difficultyMin = 0;
        int difficultyMax = 101;
        int rankMin = 1;
        int rankMax = 1;

        String[] successMessages = {"这不轻轻松松嘛~", "这鱼还没发力！", "慢慢的、慢慢的..."};
        String[] failureMessages = {"挂底了吗？", "怎么这么有劲？难道是大鱼？", "卧槽！卧槽！卧槽！"};
        String[] otherMessages = {"钓鱼就是这么简单", "一条小鱼也敢班门弄斧！", "收！收！收！~~"};
        String[] errorMessages = {"风吹的...", "眼花了...", "走神了...", "呀！切线了...", "钓鱼佬绝不空军！"};

        //随机睡眠
        try {
            Thread.sleep(RandomUtil.randomInt(5000, isFishing ? 60000 : 200000));
        } catch (InterruptedException e) {
            Log.debug(e);
        }
        subject.sendMessage(MessageUtils.newChain(new At(user.getId()), new PlainText("有动静了，快来！")));
        //开始拉扯
        boolean rankStatus = true;
        int pull = 0;
        int put = 0;
        while (rankStatus) {
            //获取下一条消息
            MessageEvent newMessage = ShareUtils.getNextMessageEventFromUser(user, subject);
            if (newMessage == null) {
                subject.sendMessage(MessageUtil.formatMessageChain(user.getId(), "你的鱼跑了！！"));
                fishInfo.switchStatus();
                return;
            }
            MessageChain nextMessage = newMessage.getMessage();
            String nextMessageCode = nextMessage.serializeToMiraiCode();
            /*
            if (!config.getPrefix().isBlank()) {
                if (!nextMessageCode.startsWith(config.getPrefix())) {
                    continue;
                }
                nextMessageCode = nextMessageCode.substring(1);
            } else {
                nextMessageCode = nextMessageCode.trim();
            }
             */
            int randomDifficultyInt = RandomUtil.randomInt(0, 4);
            int randomLevelInt = RandomUtil.randomInt(0, 4);

            int message = RandomUtil.randomInt(0, 3);
            switch (nextMessageCode) {
                case "向左拉":
                case "左":
                case "1":
                    if (randomDifficultyInt % 2 == 1) {
                        difficultyMin += 15;
                        subject.sendMessage(MessageUtil.formatMessageChain(nextMessage, successMessages[message]));
                    } else {
                        difficultyMin -= 18;
                        subject.sendMessage(MessageUtil.formatMessageChain(nextMessage, failureMessages[message]));
                    }
                    pull++;
                    break;
                case "向右拉":
                case "右":
                case "2":
                    if (randomDifficultyInt % 2 == 0) {
                        difficultyMin += 15;
                        subject.sendMessage(MessageUtil.formatMessageChain(nextMessage, successMessages[message]));
                    } else {
                        difficultyMin -= 18;
                        subject.sendMessage(MessageUtil.formatMessageChain(nextMessage, failureMessages[message]));
                    }
                    pull++;
                    break;
                case "收线":
                case "拉":
                case "0":
                    if (randomLevelInt % 2 == 0) {
                        difficultyMin += 18;
                        subject.sendMessage(MessageUtil.formatMessageChain(nextMessage, otherMessages[message]));
                    } else {
                        difficultyMin -= 20;
                        subject.sendMessage(MessageUtil.formatMessageChain(nextMessage, failureMessages[message]));
                    }
                    rankMax++;
                    pull++;
                    break;
                case "放线":
                case "放":
                case "~":
                    if (pull > 2) {
                        difficultyMin += 15;
                        rankMax = 1;
                        pull--;
                    } else {
                        rankMax--;
                    }
                    subject.sendMessage(MessageUtil.formatMessageChain(nextMessage, "你把你收回来的线，又放了出去!"));
                    break;
                default:
                    if (Pattern.matches("[!！收起提竿杆]{1,2}", nextMessageCode)) {
                        if (pull == 0) {
                            theRod = true;
                        }
                        rankStatus = false;
                    }
                    break;
            }
            pull++;
        }
        //空军
        if (theRod) {
            if (RandomUtil.randomInt(0, 101) >= 50) {
                subject.sendMessage(MessageUtil.formatMessageChain(user.getId(), errorMessages[RandomUtil.randomInt(0, 5)]));
                fishInfo.switchStatus();
                return;
            }
        }

        /*
        最小钓鱼等级 = max((钓鱼竿支持最大等级/5)+1,基础最小等级）
        最大钓鱼等级 = max(最小钓鱼等级+1,min(钓鱼竿支持最大等级,鱼塘支持最大等级,拉扯的等级))
         */
        rankMin = Math.max((fishInfo.getLevel() / 5) + 1, rankMin);
        rankMax = Math.max(rankMin + 1, Math.min(fishInfo.getLevel(), Math.min(fishPond.getPondLevel(), rankMax)));
        /*
        最小难度 = 拉扯最小难度
        最大难度 = max(拉扯最小难度,基本最大难度+鱼竿等级)
         */
        difficultyMax = Math.max(difficultyMin + 1, difficultyMax + fishInfo.getRodLevel());
        //roll等级
        int rank = RandomUtil.randomInt(rankMin, rankMax + 1);
        Log.debug("钓鱼管理:roll等级min" + rankMin);
        Log.debug("钓鱼管理:roll等级max" + rankMax);
        Log.debug("钓鱼管理:roll等级" + rank);
        Fish fish;
        //彩蛋
        boolean winning = false;
        while (true) {
            if (rank == 0) {
                subject.sendMessage(MessageUtil.formatMessageChain(user.getId(), " 切线了我去！"));
                fishInfo.switchStatus();
                return;
            }
            //roll难度
            int difficulty = RandomUtil.randomInt(difficultyMin, difficultyMax);
            Log.debug("钓鱼管理:等级:" + rank + "-roll难度min" + difficultyMin);
            Log.debug("钓鱼管理:等级:" + rank + "-roll难度max" + difficultyMax);
            Log.debug("钓鱼管理:等级:" + rank + "-roll难度" + difficulty);
            //在所有鱼中拿到对应的鱼等级
            List<Fish> levelFishList = fishPond.getFishList(rank);
            //过滤掉难度不够的鱼
            List<Fish> collect;
            collect = levelFishList.stream()
                    .filter(it -> it.getDifficulty() <= difficulty)
                    .sorted(Comparator.comparing(Fish::getDescription))
                    .collect(Collectors.toList());
            //如果没有了
            int size = collect.size();
            if (size == 0) {
                //降级重新roll难度处理
                rank--;
                continue;
            }
            //难度>=200 触发彩蛋
            if (difficulty >= 200) {
                winning = true;
            }
            //roll鱼
            fish = collect.get(RandomUtil.randomInt(0, Math.min(6, size)));
            break;
        }
        //roll尺寸
        int dimensions = fish.getDimensions(winning);
        int money = fish.getPrice() * dimensions;
        double v = money * (1 - fishPond.getRebate());
        if (EconomyUtil.plusMoneyToUser(user, v) && EconomyUtil.plusMoneyToPluginBankForId(fishPond.getCode(), fishPond.getDescription(), money * fishPond.getRebate())) {
            fishPond.addNumber();
            String format = String.format("\n起竿咯！\n%s\n等级:%s\n单价:%s\n尺寸:%d\n总金额:%d\n%s", fish.getName(), fish.getLevel(), fish.getPrice(), dimensions, money, fish.getDescription());
            MessageChainBuilder messages = new MessageChainBuilder();
            messages.append(new At(userInfo.getQq())).append(new PlainText(format));
            subject.sendMessage(messages.build());
        } else {
            subject.sendMessage("钓鱼失败!");
            playerCooling.remove(userInfo.getQq());
        }
        fishInfo.switchStatus();
        FishRanking fishRanking = new FishRanking(userInfo.getQq(), userInfo.getName(), dimensions, money, fishInfo.getRodLevel(), fish, fishPond);
        HibernateFactory.merge(fishRanking);
        TitleManager.checkFishTitle(userInfo, subject);
    }

    /**
     * 购买鱼竿
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/9 16:15
     */
    @MessageAuthorize(
            text = {"购买鱼竿"},
            groupPermissions = PermCode.FISH_PERM
    )
    public static void buyFishRod(MessageEvent event) {
        Log.info("购买鱼竿指令");

        UserInfo userInfo = UserManager.getUserInfo(event.getSender());
        User user;
        if (userInfo == null) {
            return;
        }
        user = userInfo.getUser();
        FishInfo fishInfo = userInfo.getFishInfo();

        Contact subject = event.getSubject();

        if (fishInfo.isFishRod()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), msgConfig.getRepeatPurchaseRod()));
            return;
        }

        double moneyByUser = EconomyUtil.getMoneyByUser(user);
        if (moneyByUser - 500 < 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), msgConfig.getCoinNotEnoughForRod()));
            return;
        }

        if (EconomyUtil.minusMoneyToUser(user, 500)) {
            fishInfo.setFishRod(true);
            HibernateFactory.merge(fishInfo);
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), msgConfig.getBuyFishingRodSuccess()));
        } else {
            Log.error("游戏管理:购买鱼竿失败!");
        }
    }

    /**
     * 升级鱼竿
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/11 22:27
     */
    @MessageAuthorize(
            text = {"升级鱼竿"},
            groupPermissions = PermCode.FISH_PERM
    )
    public static void upFishRod(MessageEvent event) {
        Log.info("升级鱼竿指令");

        UserInfo userInfo = UserManager.getUserInfo(event.getSender());

        Contact subject = event.getSubject();

        FishInfo fishInfo = userInfo.getFishInfo();
        if (!fishInfo.isFishRod()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), msgConfig.getNoneRodUpgradeMsg()));
            return;
        }
        if (fishInfo.getStatus()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), msgConfig.getUpgradeWhenFishing()));
            return;
        }
        SingleMessage singleMessage = fishInfo.updateRod(userInfo);
        subject.sendMessage(singleMessage);
    }


    /**
     * 钓鱼排行榜
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/14 15:27
     */
    @MessageAuthorize(
            text = {"钓鱼榜", "钓鱼排行"}
    )
    public static void fishTop(MessageEvent event) {
        Log.info("钓鱼榜指令");

        Bot bot = event.getBot();
        Contact subject = event.getSubject();

        List<FishRanking> rankingList = HibernateFactory.selectList(FishRanking.class);
        rankingList.sort(Comparator.comparing(FishRanking::getMoney).reversed());
        rankingList = rankingList.isEmpty() ? rankingList : rankingList.subList(0, Math.min(rankingList.size(), 30));

        if (rankingList.isEmpty()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "暂时没人钓鱼!"));
            return;
        }
        ForwardMessageBuilder iNodes = new ForwardMessageBuilder(subject);
        iNodes.add(bot, new PlainText("钓鱼排行榜:"));

        int start = 0;
        int end = 10;

        for (int i = start; i < end && i < rankingList.size(); i++) {
            FishRanking ranking = rankingList.get(i);
            iNodes.add(bot, ranking.getInfo(i));
        }
//        while (true) {
//          todo 钓鱼榜分页
//        }

        subject.sendMessage(iNodes.build());
    }

    /**
     * 刷新钓鱼状态
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/16 11:04
     */
    @MessageAuthorize(
            text = "刷新钓鱼",
            userPermissions = {PermConstant.OWNER, PermConstant.ADMIN},
            groupPermissions = PermCode.FISH_PERM
    )
    public static void refresh(MessageEvent event) {
        Log.info("刷新钓鱼指令");

        Boolean status = HibernateFactory.getSession().fromTransaction(session -> {
            HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
            JpaCriteriaQuery<FishInfo> query = builder.createQuery(FishInfo.class);
            JpaRoot<FishInfo> from = query.from(FishInfo.class);
            query.select(from);
            query.where(builder.equal(from.get("status"), true));
            List<FishInfo> list;
            try {
                list = session.createQuery(query).list();
            } catch (Exception e) {
                return false;
            }
            for (FishInfo fishInfo : list) {
                fishInfo.setStatus(false);
                session.merge(fishInfo);
            }
            return true;
        });
        playerCooling.clear();
        if (status) {
            event.getSubject().sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "钓鱼状态刷新成功!"));
        } else {
            event.getSubject().sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "钓鱼状态刷新失败!"));
        }
    }


    /**
     * 查看鱼竿等级
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/23 16:12
     */
    @MessageAuthorize(
            text = "鱼竿等级",
            groupPermissions = PermCode.FISH_PERM
    )
    public static void viewFishLevel(MessageEvent event) {
        Log.info("鱼竿等级指令");

        int rodLevel = Objects.requireNonNull(UserManager.getUserInfo(event.getSender())).getFishInfo().getRodLevel();
        event.getSubject().sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "你的鱼竿等级为%s级", rodLevel));
    }

    @MessageAuthorize(
            text = "开启 钓鱼",
            userPermissions = {PermConstant.OWNER, PermConstant.ADMIN}
    )
    public void startFish(GroupMessageEvent event) {
        Log.info("管理指令");

        Group group = event.getGroup();
        cn.chahuyun.authorize.entity.User user = cn.chahuyun.authorize.entity.User.Companion.group(group.getId());

        PermUtil util = PermUtil.INSTANCE;


        if (util.checkUserHasPerm(user, PermCode.FISH_PERM)) {
            group.sendMessage("本群的钓鱼已经开启了!");
            return;
        }

        if (util.addUserToPermGroupByName(user, PermCode.FISH_PERM_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群钓鱼开启成功!"));
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群钓鱼开启失败!"));
        }
    }

    @MessageAuthorize(
            text = "关闭 钓鱼",
            userPermissions = {PermConstant.OWNER, PermConstant.ADMIN}
    )
    public void offFish(GroupMessageEvent event) {
        Log.info("管理指令");

        Group group = event.getGroup();
        cn.chahuyun.authorize.entity.User user = cn.chahuyun.authorize.entity.User.Companion.group(group.getId());

        PermUtil util = PermUtil.INSTANCE;


        if (!util.checkUserHasPerm(user, PermCode.FISH_PERM)) {
            group.sendMessage("本群的钓鱼已经关闭了!");
            return;
        }

        PermGroup permGroup = util.talkPermGroupByName(PermCode.FISH_PERM_GROUP);
        permGroup.getUsers().remove(user);

        permGroup.save();

        group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群钓鱼关闭成功!"));
    }


    @MessageAuthorize(
            text = "鱼塘等级"
    )
    public void viewFishPond(GroupMessageEvent event) {
        Group group = event.getGroup();
        FishPond fishPond = UserManager.getUserInfo(event.getSender()).getFishInfo().getFishPond(group);

        int level = fishPond.getPondLevel();

        val value = FishPondLevelConstant.values()[level - 1];
        double money = fishPond.getFishPondMoney();

        group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),
                "当前鱼塘信息:%n" +
                        "鱼塘名称:%s%n" +
                        "鱼塘等级:%d%n" +
                        "鱼塘最低鱼竿等级:%d%n" +
                        "鱼塘升级所需金额:%d%n" +
                        "鱼塘金额:%.1f%n" +
                        "鱼塘升级进度:%.1f%%",
                fishPond.getName(), level, fishPond.getMinLevel(), value.getAmount(), money, (money / value.getAmount())
        ));
    }

}
