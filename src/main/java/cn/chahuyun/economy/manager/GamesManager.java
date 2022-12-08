package cn.chahuyun.economy.manager;

import cn.chahuyun.economy.entity.fish.FishInfo;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.fish.FishPond;
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
import java.util.Map;
import java.util.regex.Pattern;

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
     *
     * @param event
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

        Log.info(String.format("%s开始钓鱼",userInfo.getName()));
        boolean quit = true;
        while (quit) {
            try {
                Thread.sleep(50000);
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
                    subject.sendMessage("什么都没有,估计是"+errorMessages[randomInt]);
                    return;
                }
            }
            switch (nextMessageCode) {
                case "向左拉":
                case "左":
                case "1":

                case "向右拉":
                case "右":
                case "2":



            }
        }

    }



}
