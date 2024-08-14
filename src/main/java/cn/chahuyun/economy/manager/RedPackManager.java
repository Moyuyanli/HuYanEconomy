package cn.chahuyun.economy.manager;

import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.entity.redpack.RedPack;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.TimeConvertUtil;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.util.RandomUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.ForwardMessageBuilder;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

/**
 * 红包管理类，用于处理红包的创建、领取、查询等操作。
 */
public class RedPackManager {
    /**
     * 创建红包。
     * @param event 群消息事件
     */
    public static void create(GroupMessageEvent event) {
        try {
            Group group = event.getGroup();
            User sender = event.getSender();
            Contact subject = event.getSubject();
            String message = event.getMessage().contentToString();
            String[] info = message.split(" ");
            long money = Long.parseLong(info[1]);
            int number = Integer.parseInt(info[2]);
            String typeMsg = info[3];

            boolean random = typeMsg.contains("随机");

            if (((double) money / number) < 0.01) {
                subject.sendMessage(new At(sender.getId()).plus("\n你发的红包太小啦! 要保证每份红包金额不能低于0.01"));
                return;
            }

            if (money > EconomyUtil.getMoneyByUser(sender)) {
                subject.sendMessage(new At(sender.getId()).plus("\n你的金币不够啦!"));
                return;
            }

            RedPack redPack = new RedPack(sender.getNick()+"的红包", group.getId(), sender.getId(), money, number, random, System.currentTimeMillis());

            EconomyUtil.plusMoneyToUser(sender, -money);

            // TODO 自定义红包名字

            int id = HibernateFactory.merge(redPack).getId();

            subject.sendMessage(new At(sender.getId()).plus("\n红包创建成功！"));
            Thread.sleep(1000);
            subject.sendMessage(new PlainText(sender.getNick()+ " 发送了一个红包，请在群内领取！\n红包ID: "
                    + id
                    + "\n红包存有 "
                    + money
                    + " 枚金币\n红包数量为 "
                    + number
                    + " 个\n红包发送时间为 "
                    + TimeConvertUtil.timeConvert(redPack.getCreateTime())
                    + "\n请使用命令领取红包！\n领取命令: "
                    + HuYanEconomy.config.getPrefix()
                    + "领红包 "
                    + id
                    ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 领取红包。
     * @param event 群消息事件
     */
    public static void receive(GroupMessageEvent event) {
        Contact subject = event.getSubject();
        try {
            Group group = event.getGroup();
            User sender = event.getSender();
            String message = event.getMessage().contentToString();
            String[] info = message.split(" ");
            int id = Integer.parseInt(info[1]);
            RedPack redPack = HibernateFactory.selectOne(RedPack.class, id);
            if (redPack == null) {
                subject.sendMessage(new At(sender.getId()).plus("\n红包不存在！"));
                return;
            }

            if (redPack.getGroupId() != group.getId()) {
                subject.sendMessage(new At(sender.getId()).plus("\n该红包不在本群！"));
                return;
            }

            if (redPack.getCreateTime() + 1000 * 60 * 60 * 24 < System.currentTimeMillis()) {
                subject.sendMessage(new At(sender.getId()).plus("\n红包已过期！"));
                expireRedPack(group, redPack);
                HibernateFactory.delete(redPack);
                return;
            }

            getRedPack(sender, subject, redPack);

        } catch (Exception e) {
            subject.sendMessage("红包领取失败! 原因: "+e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询红包列表。
     * @param event 群消息事件
     */
    public static void queryRedPackList(GroupMessageEvent event) {
        Contact subject = event.getSubject();
        try {
            Group group = event.getGroup();
            Bot bot = event.getBot();
            List<RedPack> redPacks = HibernateFactory.selectList(RedPack.class, "groupId", group.getId());

            ForwardMessageBuilder forwardMessage = new ForwardMessageBuilder(subject);

            if (redPacks.isEmpty()) {
                subject.sendMessage("本群暂无红包！");
                return;
            }

            redPacks.forEach(redPack -> {
                int id = redPack.getId();
                String name = redPack.getName();
                long senderId = redPack.getSender();
                long money = redPack.getMoney();
                int number = redPack.getNumber();
                long createTime = redPack.getCreateTime();
                List<Long> receivers = redPack.getReceivers();

                Message message = new PlainText("红包信息: \n"
                        + "红包ID: " + id
                        + "\n红包名称: " + name
                        + "\n红包发送者QQ号: " + senderId
                        + "\n红包金额: " + money
                        + "\n红包人数: " + number
                        + "\n红包创建时间: " + TimeConvertUtil.timeConvert(createTime)
                        + "\n红包领取者: " + receivers
                    );
                forwardMessage.add(bot, message);
            });
            subject.sendMessage(forwardMessage.build());
        } catch (Exception e) {
            subject.sendMessage("查询失败! 原因: "+e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 领取最新红包。
     * @param event 消息事件
     */
    public static void grabNewestRedPack(GroupMessageEvent event) {
        Contact subject = event.getSubject();
        try {
            Group group = event.getGroup();
            User sender = event.getSender();
            List<RedPack> redPacks = HibernateFactory.selectList(RedPack.class, "groupId", group.getId());
            if (redPacks.isEmpty()) {
                subject.sendMessage("当前群组没有红包哦!");
                return;
            }
            redPacks.sort(Comparator.comparing(RedPack::getCreateTime).reversed());

            getRedPack(sender, subject, redPacks.get(0));
        } catch (Exception e) {
            subject.sendMessage("领取失败! 原因: "+e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取红包。
     * @param sender 发送者
     * @param subject 联系对象
     * @param redPack 红包
     */
    private static void getRedPack (User sender, Contact subject, RedPack redPack) {
        double money = redPack.getMoney();
        long number = redPack.getNumber();
        boolean isRandomPack = redPack.isRandomPack();


        List<Long> receivers = redPack.getReceivers();
        if (!receivers.isEmpty()&&receivers.contains(sender.getId())) {
            subject.sendMessage(new At(sender.getId()).plus("\n你已经领取过该红包了！"));
            return;
        }

        if (receivers.size() >= redPack.getNumber()) {
            subject.sendMessage(new At(sender.getId()).plus("\n红包已被领完！"));
            HibernateFactory.delete(redPack);
            return;
        }


        // 领取措施
        DecimalFormat df = new DecimalFormat("#.00");
        double perMoney;

        if (isRandomPack) {
            List<Double> alreadyTakenMoney = redPack.getGetMoneys();
            double alreadyTakenMoneySum = alreadyTakenMoney.stream().mapToDouble(Double::doubleValue).sum();
            Log.debug("已领走钱数: "+ redPack.getGetMoneys());
            long alreadyTakenUser = redPack.getReceivers().size();
            Log.debug("已领走人数: "+ alreadyTakenUser);
            money -= alreadyTakenMoneySum;
            Log.debug("剩余钱数: "+ money);
            if (!((number-alreadyTakenUser) == 1)) {
                money -= 0.01 * (number - alreadyTakenUser);
                perMoney = Double.parseDouble(df.format(RandomUtil.randomDouble(0.01, money)));
                Log.debug("获得钱数: "+perMoney);
            } else {
                perMoney = Double.parseDouble(df.format(money));
            }

            alreadyTakenMoney.add(perMoney);

            redPack.setGetMoneys(alreadyTakenMoney);

        } else {
            perMoney = Double.parseDouble(df.format(money / number));
        }
        EconomyUtil.plusMoneyToUser(sender, perMoney);
        receivers.add(sender.getId());
        redPack.setReceivers(receivers);
        HibernateFactory.merge(redPack);

        subject.sendMessage(new At(sender.getId()).plus("\n恭喜你领取到了一个红包，你领取了"+perMoney+"枚金币！"));

        if (receivers.size() >= redPack.getNumber()) {
            long useTime = (System.currentTimeMillis() - redPack.getCreateTime())/1000;
            subject.sendMessage("红包已被领完！共计花费" + useTime + "秒! ");
            HibernateFactory.delete(redPack);
        }
    }

    /**
     * 红包过期处理。
     * @param group 群组
     * @param redPack 红包
     */
    public static void expireRedPack(Group group, RedPack redPack) {
        long ownerId = redPack.getSender();
        long money = redPack.getMoney();
        int number = redPack.getNumber();
        int receiversNumber = redPack.getReceivers().size();

        User owner = group.get(ownerId);
        long remainingMoney = money - ((money/number)*receiversNumber);

        EconomyUtil.plusMoneyToUser(owner, remainingMoney);

        group.sendMessage(new At(ownerId).plus("\n你的红包过期啦！退还金币 "+remainingMoney+" 个！"));
    }

    /**
     * 查询全局红包列表。
     * @param event 消息事件
     */
    public static void queryGlobalRedPackList(MessageEvent event) {
        Contact subject = event.getSubject();
        try {
            Bot bot = event.getBot();
            List<RedPack> redPacks = HibernateFactory.selectList(RedPack.class);

            ForwardMessageBuilder forwardMessage = new ForwardMessageBuilder(subject);

            if (redPacks.isEmpty()) {
                subject.sendMessage("全局暂无红包！");
                return;
            }

            redPacks.forEach(redPack -> {
                int id = redPack.getId();
                String name = redPack.getName();
                long senderId = redPack.getSender();
                long money = redPack.getMoney();
                int number = redPack.getNumber();
                long createTime = redPack.getCreateTime();
                List<Long> receivers = redPack.getReceivers();

                Message message = new PlainText("红包信息: \n"
                        + "红包ID: " + id
                        + "\n红包名称: " + name
                        + "\n红包发送者QQ号: " + senderId
                        + "\n红包金额: " + money
                        + "\n红包人数: " + number
                        + "\n红包创建时间: " + TimeConvertUtil.timeConvert(createTime)
                        + "\n红包领取者: " + receivers
                );
                forwardMessage.add(bot, message);
            });
            subject.sendMessage(forwardMessage.build());
        } catch (Exception e) {
            subject.sendMessage("查询失败! 原因: "+e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
