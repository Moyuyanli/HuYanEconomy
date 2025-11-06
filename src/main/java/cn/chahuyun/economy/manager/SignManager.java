package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.AuthPerm;
import cn.chahuyun.authorize.entity.PermGroup;
import cn.chahuyun.authorize.utils.PermUtil;
import cn.chahuyun.authorize.utils.UserUtil;
import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.constant.EconPerm;
import cn.chahuyun.economy.constant.ImageDrawXY;
import cn.chahuyun.economy.constant.TitleCode;
import cn.chahuyun.economy.entity.UserBackpack;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.props.PropsCard;
import cn.chahuyun.economy.entity.props.UseEvent;
import cn.chahuyun.economy.plugin.ImageManager;
import cn.chahuyun.economy.plugin.PluginManager;
import cn.chahuyun.economy.prop.PropsManager;
import cn.chahuyun.economy.sign.SignEvent;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.ImageUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.val;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.EventKt;
import net.mamoe.mirai.event.EventPriority;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.PlainText;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 签到管理<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:25
 */
@EventComponent
public class SignManager {

    private static int index = 0;


    /**
     * 签到<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/15 14:53
     */
    @MessageAuthorize(text = {"签到", "打卡", "sign"}, blackPermissions = EconPerm.SIGN_BLACK_PERM)
    public static void sign(GroupMessageEvent event) {
        Log.info("签到指令");

        User user = event.getSender();
        Contact subject = event.getSubject();
        MessageChain message = event.getMessage();

        UserInfo userInfo = UserManager.getUserInfo(user);

        MessageChainBuilder messages = MessageUtil.quoteReply(message);

        if (!userInfo.sign()) {
            messages.append(new PlainText("你已经签到过了哦!"));
            subject.sendMessage(messages.build());
            return;
        }

        SignEvent signEvent = new SignEvent(userInfo, event);
        signEvent.setParam(RandomUtil.randomInt(0, 1001));
        signEvent.eventReplyAdd(MessageUtil.formatMessageChain(message, "本次签到触发事件:"));

        //广播签到事件
        SignEvent broadcast = EventKt.broadcast(signEvent);

        double goldNumber = broadcast.getGold();
        MessageChain reply = broadcast.getReply();
        userInfo = broadcast.getUserInfo();

        MessageChainBuilder eventReply = signEvent.getEventReply();
        if (eventReply.size() != 2) {
            subject.sendMessage(eventReply.build());
        }

        if (!EconomyUtil.plusMoneyToUser(userInfo.getUser(), goldNumber)) {
            subject.sendMessage("签到失败!");
            //todo 签到失败回滚
            return;
        }

        userInfo.setSignEarnings(goldNumber);
        HibernateFactory.merge(userInfo);

        double moneyBytUser = EconomyUtil.getMoneyByUser(userInfo.getUser());
        messages.append(new PlainText("签到成功!\n"));
        messages.append(new PlainText(String.format("金币:%s(+%s)\n", moneyBytUser, goldNumber)));
        if (reply != null) {
            messages.add(reply);
        }
        if (userInfo.getOldSignNumber() != 0) {
            messages.append(String.format("你的连签线断在了%d天,可惜~", userInfo.getOldSignNumber()));
        }

        TitleManager.checkSignTitle(userInfo, subject);
        TitleManager.checkMonopoly(userInfo, subject);

        sendSignImage(userInfo, subject, messages.build());
    }


    @MessageAuthorize(
            text = "关闭 签到",
            userPermissions = {AuthPerm.OWNER, AuthPerm.ADMIN}
    )
    public void offSign(GroupMessageEvent event) {
        Group group = event.getGroup();

        PermUtil util = PermUtil.INSTANCE;

        val user = UserUtil.INSTANCE.group(group.getId());

        if (util.checkUserHasPerm(user, EconPerm.SIGN_BLACK_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的签到已经关闭了!"));
            return;
        }

        if (util.addUserToPermGroupByName(user, EconPerm.GROUP.SIGN_BLACK_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的签到关闭成功!"));
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的签到关闭失败!"));
        }
    }


    @MessageAuthorize(
            text = "开启 签到",
            userPermissions = {AuthPerm.OWNER, AuthPerm.ADMIN}
    )
    public void startSign(GroupMessageEvent event) {
        Group group = event.getGroup();

        PermUtil util = PermUtil.INSTANCE;

        val user = UserUtil.INSTANCE.group(group.getId());

        if (!util.checkUserHasPerm(user, EconPerm.SIGN_BLACK_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的签到已经开启!"));
            return;
        }

        PermGroup permGroup = util.talkPermGroupByName(EconPerm.GROUP.SIGN_BLACK_GROUP);

        permGroup.getUsers().remove(user);
        permGroup.save();

        group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本群的签到开启成功!"));
    }


    /**
     * 签到金钱获取<br>
     * 优先级:{@link EventPriority#HIGH}
     *
     * @param event 签到事件
     */
    public static void randomSignGold(SignEvent event) {
        double goldNumber;

        MessageChainBuilder builder = new MessageChainBuilder();

        Integer param = event.getParam();

        if (param <= 500) {
            goldNumber = RandomUtil.randomInt(50, 101);
        } else if (param <= 850) {
            goldNumber = RandomUtil.randomInt(100, 201);
            builder.add(new PlainText(String.format("哇偶,你今天运气爆棚,获得%s金币", goldNumber)));
        } else if (param <= 999) {
            goldNumber = RandomUtil.randomInt(200, 501);
            builder.add(new PlainText(String.format("卧槽,你家祖坟裂了,冒出%s金币", goldNumber)));
        } else {
            goldNumber = 999;
            builder.add(new PlainText("你™直接天降神韵!"));
        }

        event.setGold(goldNumber);
        event.setReply(builder.build());
    }

    /**
     * 自定义签到事件<br>
     * 优先级:{@link EventPriority#NORMAL}
     *
     * @param event 签到事件
     */
    public static void signProp(SignEvent event) {
        UserInfo userInfo = event.getUserInfo();

        int multiples = 1;

        if (TitleManager.checkTitleIsOnEnable(userInfo, TitleCode.SIGN_15)) {
            multiples += 1;
            event.eventReplyAdd(MessageUtil.formatMessageChain("装备签到狂人称号，本次签到奖励翻倍!"));
        } else if (TitleManager.checkTitleIsOnEnable(userInfo, TitleCode.SIGN_90)) {
            multiples += 4;
            event.eventReplyAdd(MessageUtil.formatMessageChain("装备签到大王称号，本次签到奖励翻5倍!"));
        }

        List<UserBackpack> backpacks = userInfo.getBackpacks();
        ArrayList<UserBackpack> list = new ArrayList<>(backpacks);

        Class<PropsCard> cardClass = PropsCard.class;
        if (BackpackManager.checkPropInUser(userInfo, PropsCard.MONTHLY)) {
            UserBackpack prop = userInfo.getProp(PropsCard.MONTHLY);
            if (PropsManager.getProp(prop, cardClass).isStatus()) {
                event.setSign_2(true);
                event.setSign_3(true);
                multiples += 4;
                event.eventReplyAdd(MessageUtil.formatMessageChain("已启用签到月卡,本次签到奖励翻5倍!"));
            }
        }

        PropsCard card;
        for (UserBackpack backpack : list) {
            Long propId = backpack.getPropId();
            switch (backpack.getPropCode()) {
                case PropsCard.SIGN_2:
                    try {
                        card = PropsManager.deserialization(propId, cardClass);
                    } catch (Exception e) {
                        BackpackManager.delPropToBackpack(userInfo, propId);
                        continue;
                    }
                    if (event.isSign_2()) {
                        continue;
                    }
                    if (card.isStatus()) {
                        multiples += 1;
                        BackpackManager.delPropToBackpack(userInfo, propId);
                        event.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张双倍签到卡，本次签到奖励翻倍!"));
                        event.setSign_2(true);
                    }

                    break;
                case PropsCard.SIGN_3:
                    try {
                        card = PropsManager.deserialization(propId, cardClass);
                    } catch (Exception e) {
                        BackpackManager.delPropToBackpack(userInfo, propId);
                        continue;
                    }
                    if (event.isSign_3()) {
                        continue;
                    }
                    if (card.isStatus()) {
                        multiples += 2;
                        BackpackManager.delPropToBackpack(userInfo, propId);
                        event.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张三倍签到卡，本次签到奖励三翻倍!"));
                        event.setSign_3(true);
                    }
                    break;
                case PropsCard.SIGN_IN:
                    try {
                        card = PropsManager.deserialization(propId, cardClass);
                    } catch (Exception e) {
                        BackpackManager.delPropToBackpack(userInfo, propId);
                        continue;
                    }
                    if (event.isSign_in()) {
                        continue;
                    }
                    //自动使用补签卡
                    int oldSignNumber = userInfo.getOldSignNumber();

                    if (oldSignNumber == 0) {
                        break;
                    }

                    userInfo.setSignNumber(userInfo.getSignNumber() + oldSignNumber);
                    userInfo.setOldSignNumber(0);

                    UseEvent useEvent = new UseEvent(userInfo.getUser(), event.getGroup(), userInfo);
                    PropsManager.useAndUpdate(backpack, useEvent);
                    event.eventReplyAdd(MessageUtil.formatMessageChain("使用了一张补签卡，续上断掉的签到天数!"));
                    event.setSign_in(true);
            }
        }

        event.setGold(event.getGold() * multiples);
    }


    @MessageAuthorize(text = "刷新签到",
            userPermissions = {AuthPerm.OWNER, AuthPerm.ADMIN})
    public void refreshSign(GroupMessageEvent event) {
        Group group = event.getGroup();
        Member sender = event.getSender();

        UserInfo userInfo = UserManager.getUserInfo(sender);

        DateTime dateTime = DateUtil.offsetDay(userInfo.getSignTime(), -1);

        userInfo.setSignTime(dateTime);

        HibernateFactory.merge(userInfo);

        group.sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "签到刷新成功!"));
    }


    //============================================================================


    /**
     * 发送签到图片信息<p>
     * 在基础信息上添加签到信息<p>
     *
     * @param userInfo 用户信息
     * @param subject  发送者
     * @param messages 消息
     * @author Moyuyanli
     * @date 2022/12/5 16:22
     */
    private static void sendSignImage(UserInfo userInfo, Contact subject, MessageChain messages) {
        BufferedImage userInfoImageBase = UserManager.getUserInfoImageBase(userInfo);
        if (userInfoImageBase == null) {
            return;
        }
        Graphics2D graphics = ImageUtil.getG2d(userInfoImageBase);
        if (PluginManager.isCustomImage) {
            graphics.setColor(Color.BLACK);
            graphics.setFont(ImageManager.getCustomFont());
            ImageUtil.drawString(messages.contentToString(), ImageDrawXY.A_WORD.getX(), ImageDrawXY.A_WORD.getY(), 440, graphics);
        } else {
            int fontSize = 20;
            graphics.setColor(Color.black);
            AtomicInteger x = new AtomicInteger(210);
            graphics.setFont(new Font("黑体", Font.PLAIN, fontSize));
            messages.forEach(v -> {
                //写入签到信息
                graphics.drawString(v.contentToString(), 520, x.get());
                x.addAndGet(28);
            });
        }
        //释放资源
        graphics.dispose();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(userInfoImageBase, "png", stream);
        } catch (IOException e) {
            Log.error("签到管理:签到图片发送错误!", e);
            subject.sendMessage(messages);
            return;
        }
        Contact.sendImage(subject, new ByteArrayInputStream(stream.toByteArray()));
    }

    /**
     * 发送签到图片
     *
     * @param userInfo 用户信息
     * @param user     发送者
     * @param subject  发送位置
     * @author Moyuyanli
     * @date 2022/12/2 12:25
     */
    @Deprecated(since = "已废弃")
    public static void sendSignImage(UserInfo userInfo, User user, Contact subject, double money, double obtain, MessageChain messages) {
        HuYanEconomy instance = HuYanEconomy.INSTANCE;
        try {
            InputStream asStream = instance.getResourceAsStream("sign" + (index % 4 == 0 ? 4 : index % 4) + ".png");
            index++;
            if (asStream == null) {
                Log.error("签到图片获取错误!");
                return;
            }
//            System.out.println("图片名称：" + file.getName());
//            System.out.println("图片大小：" + file.length() / 1024 + " kb");
            // 将文件对象转化为图片对象
            BufferedImage image = ImageIO.read(asStream);

//            System.out.println("图片宽度：" + image.getWidth() + " px");
//            System.out.println("图片高度：" + image.getHeight() + " px");

            // 创建画笔(image为上一步的图片对象)
            Graphics2D pen = ImageUtil.getG2d(image);

            String avatarUrl = user.getAvatarUrl(AvatarSpec.LARGE);
            BufferedImage avatar = ImageIO.read(new URL(avatarUrl));
            //圆角处理
//            BufferedImage avatarRounder = makeRoundedCorner(avatar, 50);
            //写入头像
//            pen.drawImage(avatarRounder, 24, 25, null);
//            ByteArrayOutputStream avatarImage = new ByteArrayOutputStream();
//            ImageIO.write(avatarRounder, "png", avatarImage);
            //发送处理后的头像
//            Contact.sendImage(subject, new ByteArrayInputStream(avatarImage.toByteArray()));

            /*
            相关说明：
            （1） pen.setColor(Color.WHITE);
            这行代码的意思是将画笔颜色设置为白色。
            其他颜色还有：WHITE(白色)、LIGHT_GRAY（浅灰色）、GRAY（灰色）、DARK_GRAY（深灰色）、
            BLACK（黑色）、RED（红色）、PINK（粉红色）、ORANGE（橘黄色）、YELLOW（黄色）、GREEN（绿色）、
            MAGENTA（紫红色）、CYAN（青色）、BLUE（蓝色）
            如果上面颜色都不满足你，或者你还想设置下字体透明度，你可以改为如下格式：
            pen.setColor(new Color(179, 250, 233, 100));
            这里的四个参数分别为 RGBA（不懂RGBA的点这里），四个参数的范围均是0-255；
            （2）pen.setFont(new Font("微软雅黑", Font.ITALIC, 20));
            Font.PLAIN（正常），Font.BOLD（粗体），Font.ITALIC（斜体）
             */
            // 设置画笔颜色为白色
            pen.setColor(Color.WHITE);
            // 设置画笔字体样式为微软雅黑，斜体，文字大小为20px
            pen.setFont(new Font("黑体", Font.BOLD, 60));
            pen.drawString(userInfo.getName(), 200, 155);
            //写入金币
            if (String.valueOf(money).length() > 5) {
                pen.setFont(new Font("黑体", Font.PLAIN, 24));
                pen.setColor(Color.black);
                pen.drawString(String.valueOf(money), 600, 410);
                pen.setFont(new Font("黑体", Font.PLAIN, 28));
            } else {
                pen.setFont(new Font("黑体", Font.PLAIN, 28));
                pen.setColor(Color.black);
                pen.drawString(String.valueOf(money), 600, 410);
            }
            pen.drawString(String.valueOf(userInfo.getQq()), 172, 240);
            pen.drawString(String.valueOf(obtain), 810, 410);
            pen.setFont(new Font("黑体", Font.PLAIN, 23));
            pen.drawString(DateUtil.format(userInfo.getSignTime(), "yyyy-MM-dd HH:mm:ss"), 172, 320);
            pen.drawString(String.valueOf(userInfo.getSignNumber()), 172, 360);
            pen.drawString(DateUtil.format(DateUtil.offsetDay(userInfo.getSignTime(), 1), "yyyy-MM-dd HH:mm:ss"), 221, 402);
            pen.drawString("暂无", 172, 440);
            AtomicInteger x = new AtomicInteger(210);
            pen.setFont(new Font("黑体", Font.PLAIN, 22));
            messages.forEach(v -> {
                pen.drawString(v.contentToString(), 520, x.get());
                x.addAndGet(28);
            });

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", stream);
            Contact.sendImage(subject, new ByteArrayInputStream(stream.toByteArray()));
        } catch (IOException e) {
            Log.error(e);
        }
    }

    /**
     * 将签到图片删除并重新复制
     *
     * @author Moyuyanli
     * @date 2022/12/1 16:11
     */
    private static void refreshSignImage() {
        HuYanEconomy instance = HuYanEconomy.INSTANCE;
        InputStream asStream = instance.getResourceAsStream("sign" + (index % 4 == 0 ? 4 : index % 4) + ".png");
        index++;
        File file = instance.resolveDataFile("sign.png");
        if (file.exists()) {
            if (file.delete()) {
                Log.debug("签到管理:签到图片刷新成功");
            }
        }
        assert asStream != null;
        try {
            Files.copy(asStream, file.toPath());
        } catch (IOException e) {
            Log.error("签到管理:签到图片刷新失败", e);
        }
    }


}
