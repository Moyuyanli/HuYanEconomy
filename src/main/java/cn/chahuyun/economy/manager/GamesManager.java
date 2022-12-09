package cn.chahuyun.economy.manager;

import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.fish.Fish;
import cn.chahuyun.economy.entity.fish.FishInfo;
import cn.chahuyun.economy.entity.fish.FishPond;
import cn.chahuyun.economy.util.EconomyUtil;
import cn.chahuyun.economy.util.Log;
import cn.chahuyun.economy.util.ShareUtils;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.PlainText;

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
        //获取玩家钓鱼信息
        FishInfo fishInfo = userInfo.getFishInfo();
        //能否钓鱼
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
        //获取鱼塘
        FishPond fishPond = fishInfo.getFishPond();
        if (fishPond == null) {
            subject.sendMessage("默认鱼塘不存在!");
            return;
        }
        //获取鱼塘限制鱼竿最低等级
        int minLevel = fishPond.getMinLevel();
        if (fishInfo.getRodLevel() < minLevel) {
            subject.sendMessage("你的鱼竿太拉了，这里不让你来，升升级吧...");
            return;
        }
        //开始钓鱼
        String start = String.format("%s开始钓鱼", userInfo.getName());
        subject.sendMessage(start);
        Log.info(start);

        //初始钓鱼信息
        boolean quit = true;
        boolean theRod = false;
        int difficultyMin = 0;
        int difficultyMax = 101;
        int rankMin = 1;
        int rankMax = 1;

        String[] successMessages = {"这不轻轻松松嘛~", "这鱼还没发力！", "慢慢的、慢慢的..."};
        String[] failureMessages = {"挂底了吗？", "怎么这么有劲？难道是大鱼？", "卧槽！卧槽！卧槽！"};
        String[] otherMessages = {"钓鱼就是这么简单", "一条小鱼也敢班门弄斧！", "收！收！收！~~"};
        String[] errorMessages = {"风吹的...", "眼花了...", "走神了..."};

        while (quit) {
            //随机睡眠
            try {
                Thread.sleep(RandomUtil.randomInt(5000, 200000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            subject.sendMessage(MessageUtils.newChain(new At(user.getId()), new PlainText("有动静了，快来！")));
            //开始拉扯
            boolean rank = true;
            int pull = 0;
            while (rank) {
                //获取下一条消息
                MessageEvent newMessage = ShareUtils.getNextMessageEventFromUser(user, false);
                String nextMessageCode = newMessage.getMessage().serializeToMiraiCode();
                int randomInt = RandomUtil.randomInt(0, 3);
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
                    case "收":
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
                        difficultyMin += 20;
                        rankMax = 1;
                        subject.sendMessage("你把你收回来的线，又放了出去!");
                        break;
                    default:
                        if (Pattern.matches("[!！收起提竿杆]{1,2}", nextMessageCode)) {
                            if (pull == 0) {
                                theRod = true;
                            }
                            rank = false;
                            quit = false;
                        }
                        break;
                }
                pull++;
            }
        }
        //空军
        if (theRod) {
            subject.sendMessage(errorMessages[RandomUtil.randomInt(0, 3)]);
        }

        /*
        最小钓鱼等级 = 1
        最大钓鱼等级 = max(最小钓鱼等级,min(钓鱼竿支持最大等级,鱼塘支持最大等级,拉扯的等级))
         */
        rankMax = Math.max(rankMin + 1, Math.min(fishInfo.getLevel(), Math.min(fishPond.getPondLevel(), rankMax)));
        /*
        最小难度 = 拉扯最小难度
        最大难度 = max(拉扯最小难度,基本最大难度+鱼竿等级)
         */
        difficultyMax = Math.max(difficultyMin + 1, difficultyMax + fishInfo.getRodLevel());
        //roll等级
        int rank = RandomUtil.randomInt(rankMin, rankMax);

        Fish fish;
        //彩蛋
        boolean winning = false;
        while (true) {
            //roll难度
            int difficulty = RandomUtil.randomInt(difficultyMin, difficultyMax);
            //在所有鱼中拿到对应的鱼等级
            List<Fish> levelFishList = fishPond.getFishList(rank);
            //过滤掉难度不够的鱼
            List<Fish> collect = levelFishList.stream().filter(it -> it.getDifficulty() <= difficulty).collect(Collectors.toList());
            //如果没有了
            if (collect.size() == 0) {
                //降级重新roll难度处理
                rank--;
                continue;
            }
            //难度>=200 触发彩蛋
            if (difficulty >= 200) {
                winning = true;
            }
            //roll鱼
            fish = collect.get(RandomUtil.randomInt(0, collect.size()));
            break;
        }
        //roll尺寸
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
