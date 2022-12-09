package cn.chahuyun.economy.manager;

import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.fish.Fish;
import cn.chahuyun.economy.entity.fish.FishInfo;
import cn.chahuyun.economy.entity.fish.FishPond;
import cn.chahuyun.economy.plugin.FishManager;
import cn.chahuyun.economy.util.EconomyUtil;
import cn.chahuyun.economy.util.Log;
import cn.chahuyun.economy.util.ShareUtils;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 游戏管理<p>
 * 24点|钓鱼<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:26
 */
public class GamesManager {

    /**
     * 玩家钓鱼冷却
     */
    private static final Map<Long, Date> playerCooling = new HashMap<>();

    private GamesManager() {
    }

    /**
     * 开始钓鱼游戏
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/9 16:16
     */
    public static void fishing(MessageEvent event) {
        UserInfo userInfo = UserManager.getUserInfo(event.getSender());
        User user = userInfo.getUser();

        Contact subject = event.getSubject();

        MessageChain message = event.getMessage();

        FishInfo fishInfo = userInfo.getFishInfo();
        if (!fishInfo.isFishRod()) {
            subject.sendMessage("你连鱼竿都没得，拿**钓？");
            return;
        }
        //钓鱼冷却
        if (playerCooling.containsKey(userInfo.getId())) {
            Date date = playerCooling.get(userInfo.getId());
            long between = DateUtil.between(date, new Date(), DateUnit.MINUTE, true);
            if (between <= 10) {
                subject.sendMessage(String.format("你还差%s分钟来准备好钓鱼!", 10 - between));
                return;
            }
        }

        FishPond fishPond = fishInfo.getFishPond();
        if (fishPond == null) {
            subject.sendMessage("默认鱼塘不存在!");
            return;
        }

        int minLevel = fishPond.getMinLevel();
        if (fishInfo.getRodLevel() < minLevel) {
            subject.sendMessage("你的鱼竿太拉了，升升级吧...");
            return;
        }

        Log.info(String.format("%s开始钓鱼", userInfo.getName()));
        boolean quit = true;
        int difficultyMin = 0;
        int difficultyMax = 100;
        int rankMin = 0;
        int rankMax = 0;
        String[] successMessages = {"这不轻轻松松嘛~", "这鱼还没发力！", "慢慢的、慢慢的..."};
        String[] failureMessages = {"挂底了吗？", "怎么这么有劲？难道是大鱼？", "卧槽！卧槽！卧槽！"};
        String[] otherMessages = {"钓鱼就是这么简单", "一条小鱼也敢班门弄斧！", "收！收！收！~~"};

        while (quit) {
            try {
                Thread.sleep(RandomUtil.randomInt(20000, 80000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            subject.sendMessage("有动静!");
            MessageEvent newMessage = ShareUtils.getNextMessageEventFromUser(user, true);
            String nextMessageCode = newMessage.getMessage().serializeToMiraiCode();
            int randomInt = RandomUtil.randomInt(0, 2);
            if (RandomUtil.randomInt(0, 10) == 0) {
                String[] errorMessages = {"风吹的...", "眼花了...", "走神了..."};
                if (Pattern.matches("[向左右拉]+", nextMessageCode)) {
                    subject.sendMessage(errorMessages[randomInt]);
                    continue;
                } else if (nextMessageCode.equals("收杆")) {
                    subject.sendMessage("什么都没有,估计是" + errorMessages[randomInt]);
                    return;
                }
            }
            switch (nextMessageCode) {
                case "向左拉":
                case "左":
                case "1":
                    if (randomInt == 1) {
                        difficultyMin += 5;
                        subject.sendMessage(successMessages[randomInt]);
                    } else {
                        difficultyMin -= 5;
                        subject.sendMessage(failureMessages[randomInt]);
                    }
                    quit = false;
                    break;
                case "向右拉":
                case "右":
                case "2":
                    if (randomInt == 2) {
                        difficultyMin += 5;
                        subject.sendMessage(successMessages[randomInt]);
                    } else {
                        difficultyMin -= 5;
                        subject.sendMessage(failureMessages[randomInt]);
                    }
                    quit = false;
                    break;
                case "收线":
                case "拉":
                case "0":
                    if (randomInt == 0) {
                        difficultyMin += 5;
                        subject.sendMessage(otherMessages[randomInt]);
                    } else {
                        difficultyMin -= 5;
                        subject.sendMessage(failureMessages[randomInt]);
                    }
                    rankMax++;
                    quit = false;
                    break;
            }
        }
        while (true) {
            MessageEvent newMessage = ShareUtils.getNextMessageEventFromUser(user, true);
            String nextMessageCode = newMessage.getMessage().serializeToMiraiCode();
            int randomInt = RandomUtil.randomInt(0, 2);
            boolean rank = false;
            switch (nextMessageCode) {
                case "向左拉":
                case "左":
                case "1":
                    if (randomInt == 1) {
                        difficultyMin += 5;
                        subject.sendMessage(successMessages[randomInt]);
                    } else {
                        difficultyMin -= 10;
                        subject.sendMessage(failureMessages[randomInt]);
                    }
                    break;
                case "向右拉":
                case "右":
                case "2":
                    if (randomInt == 2) {
                        difficultyMin += 5;
                        subject.sendMessage(successMessages[randomInt]);
                    } else {
                        difficultyMin -= 10;
                        subject.sendMessage(failureMessages[randomInt]);
                    }
                    break;
                case "收线":
                case "拉":
                case "0":
                    if (randomInt == 0) {
                        difficultyMin += 5;
                        subject.sendMessage(otherMessages[randomInt]);
                    } else {
                        difficultyMin -= 10;
                        subject.sendMessage(failureMessages[randomInt]);
                    }
                    rankMax++;
                    break;
                case "放线":
                case "放":
                case "~":
                    difficultyMin = 0;
                    rankMax = 0;
                    break;
                case "收竿":
                case "收杆":
                case "提杆":
                case "提竿":
                case "！":
                case "!":
                    rank = true;
                    break;
            }
            if (rank) {
                break;
            }
        }
        rankMax = Math.min(rankMax, Math.min(fishInfo.getLevel(), fishPond.getPondLevel()));
        difficultyMax = difficultyMax + fishInfo.getRodLevel();
        int rank = RandomUtil.randomInt(rankMin, rankMax);
        Fish fish;
        boolean winning = false;
        while (true) {
            int difficulty = RandomUtil.randomInt(difficultyMin, difficultyMax);
            List<Fish> levelFishList = FishManager.getLevelFishList(rank);
            List<Fish> collect = levelFishList.stream().filter(it -> it.getDifficulty() <= difficulty).collect(Collectors.toList());
            if (collect.size() == 0) {
                rank--;
                continue;
            }
            if (difficulty == 200) {
                winning = true;
            }
            fish = collect.get(RandomUtil.randomInt(0, collect.size()));
            break;
        }
        int dimensions = fish.getDimensions(winning);
        int money = fish.getPrice() * dimensions;
        String format = String.format("起竿咯！\n%s\n%s\n尺寸:%d\n总金额:%d", fish.getName(), fish.getDescription(), dimensions, money);
        subject.sendMessage(format);
    }

    /**
     * 购买鱼竿
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/9 16:15
     */
    public static void buyFishRod(MessageEvent event) {
        UserInfo userInfo = UserManager.getUserInfo(event.getSender());
        User user = userInfo.getUser();
        FishInfo fishInfo = userInfo.getFishInfo();

        Contact subject = event.getSubject();

        if (fishInfo.isFishRod()) {
            subject.sendMessage("你已经有一把钓鱼竿了，不用再买了！");
            return;
        }

        double moneyByUser = EconomyUtil.getMoneyByUser(user);
        if (moneyByUser - 500 < 0) {
            subject.sendMessage("我这把钓鱼竿可是神器！他能吸收你的金币来进化，卖你500还嫌贵？");
            return;
        }

        if (EconomyUtil.lessMoneyToUser(user, 500)) {
            fishInfo.setFishRod(true);
            fishInfo.save();
            subject.sendMessage("拿好了，这鱼竿到手即不负责，永不提供售后！");
        } else {
            Log.error("游戏管理:购买鱼竿失败!");
        }
    }

}
