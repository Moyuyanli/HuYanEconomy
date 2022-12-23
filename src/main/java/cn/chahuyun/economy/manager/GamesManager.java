package cn.chahuyun.economy.manager;

import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.fish.Fish;
import cn.chahuyun.economy.entity.fish.FishInfo;
import cn.chahuyun.economy.entity.fish.FishPond;
import cn.chahuyun.economy.entity.fish.FishRanking;
import cn.chahuyun.economy.utils.*;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

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
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"你连鱼竿都没得，拿**钓？"));
            return;
        }
        //是否已经在钓鱼
        if (fishInfo.getStatus()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"你已经在钓鱼了！"));
            return;
        }
        //钓鱼冷却
        if (playerCooling.containsKey(userInfo.getQq())) {
            Date date = playerCooling.get(userInfo.getQq());
            long between = DateUtil.between(date, new Date(), DateUnit.MINUTE, true);
            if (between < 10) {
                subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"你还差%s分钟来抛第二杆!", 10 - between));
                return;
            } else {
                playerCooling.remove(userInfo.getQq());
            }
        } else {
            playerCooling.put(userInfo.getQq(), new Date());
        }
        //是否已经在钓鱼
        if (fishInfo.isStatus()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"你已经在钓鱼了！"));
            return;
        }
        //获取鱼塘
        FishPond fishPond = fishInfo.getFishPond();
        if (fishPond == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"默认鱼塘不存在!"));
            return;
        }
        //获取鱼塘限制鱼竿最低等级
        int minLevel = fishPond.getMinLevel();
        if (fishInfo.getRodLevel() < minLevel) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"你的鱼竿太拉了，这里不让你来，升升级吧..."));
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
            Thread.sleep(RandomUtil.randomInt(5000, 200000));
        } catch (InterruptedException e) {
            Log.debug(e);
        }
        subject.sendMessage(MessageUtils.newChain(new At(user.getId()), new PlainText("有动静了，快来！")));
        //开始拉扯
        boolean rankStatus = true;
        int pull = 0;
        while (rankStatus) {
            //获取下一条消息
            MessageEvent newMessage = ShareUtils.getNextMessageEventFromUser(user, subject, false);
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
                        difficultyMin -= 8;
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
                        difficultyMin -= 8;
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
                        difficultyMin -= 12;
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
                        rankStatus = false;
                    }
                    break;
            }
            pull++;
        }
        //空军
        if (theRod) {
            if (RandomUtil.randomInt(0, 101) >= 50) {
                subject.sendMessage(errorMessages[RandomUtil.randomInt(0, 5)]);
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
                subject.sendMessage("切线了我去！");
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
            collect = levelFishList.stream().filter(it -> it.getDifficulty() <= difficulty).collect(Collectors.toList());
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
            fish = collect.get(RandomUtil.randomInt(size > 6 ? size - 6 : 0, size));
            break;
        }
        //roll尺寸
        int dimensions = fish.getDimensions(winning);
        int money = fish.getPrice() * dimensions;
        double v = money * (1 - fishPond.getRebate());
        if (EconomyUtil.plusMoneyToUser(user, v) && EconomyUtil.plusMoneyToBankForId(fishPond.getCode(), fishPond.getDescription(), money * fishPond.getRebate())) {
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
        new FishRanking(userInfo.getQq(), userInfo.getName(), dimensions, money, fishInfo.getRodLevel(), fish, fishPond).save();
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
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"你已经有一把钓鱼竿了，不用再买了！"));
            return;
        }

        double moneyByUser = EconomyUtil.getMoneyByUser(user);
        if (moneyByUser - 500 < 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"我这把钓鱼竿可是神器！他能吸收你的金币来进化，卖你500还嫌贵？"));
            return;
        }

        if (EconomyUtil.minusMoneyToUser(user, 500)) {
            fishInfo.setFishRod(true);
            fishInfo.save();
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"拿好了，这鱼竿到手即不负责，永不提供售后！"));
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
    public static void upFishRod(MessageEvent event) {
        UserInfo userInfo = UserManager.getUserInfo(event.getSender());

        Contact subject = event.getSubject();

        FishInfo fishInfo = userInfo.getFishInfo();
        if (!fishInfo.isFishRod()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"鱼竿都没得，你升级个锤子!"));
            return;
        }
        if (fishInfo.getStatus()) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"钓鱼期间不可升级鱼竿!"));
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
    public static void fishTop(MessageEvent event) {
        Bot bot = event.getBot();
        Contact subject = event.getSubject();
        UserInfo userInfo = UserManager.getUserInfo(event.getSender());
        User user = userInfo.getUser();

        List<FishRanking> rankingList = HibernateUtil.factory.fromSession(session -> {
            HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
            JpaCriteriaQuery<FishRanking> query = builder.createQuery(FishRanking.class);
            JpaRoot<FishRanking> from = query.from(FishRanking.class);
            query.select(from);
            query.orderBy(builder.desc(from.get("money")));
            List<FishRanking> list = session.createQuery(query).list();
            if (list.size() == 0) {
                return null;
            }
            return list.subList(0, Math.min(list.size(), 30));
        });
        if (rankingList == null || rankingList.size() == 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"暂时没人钓鱼!"));
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
    public static void refresh(MessageEvent event) {
        Boolean status = HibernateUtil.factory.fromTransaction(session -> {
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
            event.getSubject().sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"钓鱼状态刷新成功!"));
        } else {
            event.getSubject().sendMessage(MessageUtil.formatMessageChain(event.getMessage(),"钓鱼状态刷新成功!"));
        }
    }


}
