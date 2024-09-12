package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.AuthPerm;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.constant.EconPerm;
import cn.chahuyun.economy.entity.redpack.RedPack;
import cn.chahuyun.economy.utils.*;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.ForwardMessageBuilder;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import java.text.DecimalFormat;
import java.util.*;

/**
 * 红包管理类，用于处理红包的创建、领取、查询等操作。
 */
@EventComponent
public class RedPackManager {

    /**
     * 创建红包。
     *
     * @param event 群消息事件
     */
    @MessageAuthorize(
            text = "发红包( \\d+){2}( (sj|随机))?",
            messageMatching = MessageMatchingEnum.REGULAR,
            groupPermissions = EconPerm.RED_PACKET_PERM
    )
    public static void create(GroupMessageEvent event) {
        Log.info("发红包指令");

        Group group = event.getGroup();
        User sender = event.getSender();
        Contact subject = event.getSubject();
        MessageChain message = event.getMessage();
        String content = message.contentToString();

        String[] info = content.split(" ");
        double money = Double.parseDouble(info[1]);
        int number = Integer.parseInt(info[2]);
        boolean random = false;
        if (info.length == 4) {
            random = info[3].equals("sj") || info[3].equals("随机");
        }

        if ((money / number) < 0.1) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你发的红包太小了,每个红包金额低于了0.1！"));
            return;
        }

        if (money > EconomyUtil.getMoneyByUser(sender)) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你的金币不够啦！"));
            return;
        }

        if (!EconomyUtil.plusMoneyToUser(sender, -money)) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "红包发送失败!"));
            return;
        }

        RedPack pack = RedPack.builder().name(sender.getNick() + "的红包")
                .groupId(group.getId())
                .sender(sender.getId())
                .money(money)
                .number(number)
                .isRandomPack(random)
                .createTime(new Date()).build();

        // TODO 自定义红包名字

        if (random) {
            //个数
            int residual = pack.getNumber();
            //总额
            double residualMoney = pack.getMoney();
            // 最小金额
            double coefficient = 0.5;

            //随机一下，30%的概率全随机红包，70%的概率稍微均分红包
            if (RandomUtil.randomInt(0, 11) <= 7) {
                //拿总金额 / 红包个数 = 均分金额
                long round = Math.round(residualMoney / residual);
                // 最小保底金额 = 5 或者 均分金额/5 ~ 均分金额*5
                coefficient = ShareUtils.rounding(RandomUtil.randomDouble(Math.min(5, round * 0.3), round * 0.9));
            }

            ArrayList<Double> doubles = new ArrayList<>();
            //循环红包个数到最后一个，最后一个直接拿余额
            for (int i = 1; i < pack.getNumber(); i++) {
                //从 0.1 到 红包总金额-每个红包的平均金额*红包个数 取随机值
                double v = RandomUtil.randomDouble(0.1, residualMoney - ((residual - i) * coefficient));
                v = ShareUtils.rounding(v);
                //添加到红包列表
                doubles.add(v);
                //红包金额减去刚刚随机的红包金额
                residualMoney -= v;
            }
            doubles.add(residualMoney);
            pack.setRandomPackList(doubles);
        }

        int id = HibernateFactory.merge(pack).getId();

        if (id == 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "红包创建失败!"));
            return;
        }

        if (random) {
            subject.sendMessage(MessageUtil.formatMessageChain(sender.getId(), "随机红包创建成功!"));
        }


        String prefix = HuYanEconomy.config.getPrefix();
        subject.sendMessage(MessageUtil.formatMessageChain(
                "%s 发送了一个红包,快来抢红包吧！%n" +
                        "红包ID:%d%n" +
                        "红包有 %.1f 枚金币%n" +
                        "红包个数%d%n" +
                        "红包发送时间%s%n" +
                        "领取命令 %s领红包 %d",
                sender.getNick(), id, money, number, TimeConvertUtil.timeConvert(pack.getCreateTime()),
                prefix.isBlank() ? "" : prefix, id
        ));
    }

    /**
     * 领取红包。
     *
     * @param event 群消息事件
     */
    @MessageAuthorize(
            text = "领红包 \\d+|收红包 \\d+",
            messageMatching = MessageMatchingEnum.REGULAR,
            groupPermissions = EconPerm.RED_PACKET_PERM
    )
    public static void receive(GroupMessageEvent event) {
        Log.info("收红包指令");

        Contact subject = event.getSubject();
        Group group = event.getGroup();
        User sender = event.getSender();
        MessageChain message = event.getMessage();
        String content = message.contentToString();

        String[] info = content.split(" ");
        int id = Integer.parseInt(info[1]);

        RedPack redPack = HibernateFactory.selectOne(RedPack.class, id);

        if (redPack == null) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "红包不存在!"));
            return;
        }

        if (redPack.getGroupId() != group.getId()) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "这不是这个群的红包!"));
            return;
        }

        if (DateUtil.between(redPack.getCreateTime(), new Date(), DateUnit.DAY) >= 1) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "这个红包已经过期了"));
            expireRedPack(group, redPack);
            HibernateFactory.delete(redPack);
            return;
        }

        getRedPack(sender, subject, redPack, message);
    }

    /**
     * 查询红包列表。
     *
     * @param event 群消息事件
     */
    @MessageAuthorize(
            text = "红包列表",
            groupPermissions = EconPerm.RED_PACKET_PERM)
    public static void queryRedPackList(GroupMessageEvent event) {
        Log.info("红包查询指令");

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

            viewRedPack(subject, bot, redPacks, forwardMessage);
        } catch (Exception e) {
            subject.sendMessage("查询失败! 原因: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("all")
    private static void viewRedPack(Contact subject, Bot bot, List<RedPack> redPacks, ForwardMessageBuilder forwardMessage) {
        if (!(subject instanceof Group)) {
            return;
        }
        Group group = (Group) subject;
        redPacks.forEach(redPack -> {
            int id = redPack.getId();
            String name = redPack.getName();
            long senderId = redPack.getSender();
            double money = redPack.getMoney();
            int number = redPack.getNumber();
            Date createTime = redPack.getCreateTime();
            List<Long> receivers = redPack.getReceiverList();

            ArrayList<String> nickNames = new ArrayList<>();
            for (Long receiver : receivers) {
                String nameCard = Objects.requireNonNull(group.get(receiver)).getNameCard();
                nickNames.add(nameCard != null ? nameCard : group.get(receiver).getNick());
            }

            Message message = new PlainText("红包信息: \n"
                    + "红包ID: " + id
                    + "\n红包名称: " + name
                    + "\n红包发送者QQ号: " + senderId
                    + "\n红包金币: " + money
                    + "\n剩余金币: " + (money - redPack.getTakenMoneys())
                    + "\n红包人数: " + number
                    + "\n红包创建时间: " + TimeConvertUtil.timeConvert(createTime)
                    + "\n红包领取者: " + nickNames
            );
            forwardMessage.add(bot, message);
        });
        subject.sendMessage(forwardMessage.build());
    }

    /**
     * 领取最新红包。
     *
     * @param event 消息事件
     */
    @MessageAuthorize(
            text = "抢红包",
            groupPermissions = EconPerm.RED_PACKET_PERM)
    public static void grabNewestRedPack(GroupMessageEvent event) {
        Log.info("抢红包指令");

        Contact subject = event.getSubject();
        try {
            Group group = event.getGroup();
            User sender = event.getSender();
            List<RedPack> redPacks = HibernateFactory.selectList(RedPack.class, "groupId", group.getId());
            if (redPacks.isEmpty()) {
                subject.sendMessage("当前群没有红包哦!");
                return;
            }
            redPacks.sort(Comparator.comparing(RedPack::getCreateTime).reversed());

            getRedPack(sender, subject, redPacks.get(0), event.getMessage());
        } catch (Exception e) {
            subject.sendMessage("领取失败! 原因: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取红包。
     *
     * @param sender  发送者
     * @param subject 联系对象
     * @param redPack 红包
     */
    private static void getRedPack(User sender, Contact subject, RedPack redPack, MessageChain message) {
        double money = redPack.getMoney();
        long number = redPack.getNumber();
        boolean isRandomPack = redPack.isRandomPack();

        List<Long> receivers = redPack.getReceiverList();
        if (!receivers.isEmpty() && receivers.contains(sender.getId())) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "你已经领取过该红包了！"));
            return;
        }

        if (receivers.size() >= redPack.getNumber()) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "红包已被领完！"));
            HibernateFactory.delete(redPack);
            return;
        }


        // 领取措施
        DecimalFormat df = new DecimalFormat("#.0");
        double perMoney;

        if (isRandomPack) {
            perMoney = redPack.getRandomPack();
            redPack.setTakenMoneys(redPack.getTakenMoneys() + perMoney);
        } else {
            perMoney = Double.parseDouble(df.format(money / number));
        }
        if (!EconomyUtil.plusMoneyToUser(sender, perMoney)) {
            subject.sendMessage(MessageUtil.formatMessageChain(message, "红包领取失败!"));
            return;
        }

        receivers.add(sender.getId());
        redPack.setReceiverList(receivers);
        HibernateFactory.merge(redPack);

        subject.sendMessage(MessageUtil.formatMessageChain(message, "恭喜你领取到了一个红包，你领取了 %.1f 枚金币！", perMoney));

        if (receivers.size() >= redPack.getNumber()) {
            long between = DateUtil.between(new Date(), redPack.getCreateTime(), DateUnit.SECOND);
            subject.sendMessage(MessageUtil.formatMessageChain("%s已被领完！共计花费%d秒!", redPack.getName(), between));
            HibernateFactory.delete(redPack);
        }
    }

    /**
     * 红包过期处理。
     *
     * @param group   群组
     * @param redPack 红包
     */
    public static void expireRedPack(Group group, RedPack redPack) {
        long ownerId = redPack.getSender();
        double money = redPack.getMoney();

        User owner = group.get(ownerId);
        double remainingMoney = money - redPack.getTakenMoneys();

        EconomyUtil.plusMoneyToUser(owner, remainingMoney);

        group.sendMessage(MessageUtil.formatMessageChain(ownerId, "你的红包过期啦！退还金币 %.1f 个！", remainingMoney));
    }

    /**
     * 查询全局红包列表。
     *
     * @param event 消息事件
     */
    @MessageAuthorize(
            text = "全局红包列表",
            userPermissions = AuthPerm.OWNER
    )
    public static void queryGlobalRedPackList(MessageEvent event) {
        Log.info("红包查看指令");

        Contact subject = event.getSubject();
        try {
            Bot bot = event.getBot();
            List<RedPack> redPacks = HibernateFactory.selectList(RedPack.class);

            ForwardMessageBuilder forwardMessage = new ForwardMessageBuilder(subject);

            if (redPacks.isEmpty()) {
                subject.sendMessage("全局暂无红包！");
                return;
            }

            viewRedPack(subject, bot, redPacks, forwardMessage);
        } catch (Exception e) {
            subject.sendMessage("查询失败! 原因: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
