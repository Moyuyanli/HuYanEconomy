package cn.chahuyun.manager;

import cn.chahuyun.HuYanEconomy;
import cn.chahuyun.constant.Constant;
import cn.chahuyun.entity.UserInfo;
import cn.chahuyun.entity.props.PropsCard;
import cn.chahuyun.plugin.PluginManager;
import cn.chahuyun.util.EconomyUtil;
import cn.chahuyun.util.Log;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import net.mamoe.mirai.contact.AvatarSpec;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.QuoteReply;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 签到管理<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:25
 */
public class SignManager {

    private static int index = 0;

    private SignManager() {

    }

    /**
     * 签到<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/15 14:53
     */
    public static void sign(MessageEvent event) {
        User user = event.getSender();
        Contact subject = event.getSubject();
        MessageChain message = event.getMessage();

        UserInfo userInfo = UserManager.getUserInfo(user);

        MessageChainBuilder messages = new MessageChainBuilder();
        messages.append(new QuoteReply(message));
        if (userInfo == null) {
            subject.sendMessage("签到失败!");
            return;
        }
        if (!userInfo.sign()) {
            messages.append(new PlainText("你今天已经签到过了哦!"));
            subject.sendMessage(messages.build());
            return;
        }

        double goldNumber;

        PlainText plainText = null;

        int randomNumber = RandomUtil.randomInt(0, 10);
        if (randomNumber > 7) {
            randomNumber = RandomUtil.randomInt(0, 10);
            if (randomNumber > 8) {
                goldNumber = RandomUtil.randomInt(200, 500);
                plainText = new PlainText(String.format("卧槽,你家祖坟裂了,冒出%s金币", goldNumber));
            } else {
                goldNumber = RandomUtil.randomInt(100, 200);
                plainText = new PlainText(String.format("哇偶,你今天运气爆棚,获得%s金币", goldNumber));
            }
        } else {
            goldNumber = RandomUtil.randomInt(50, 100);
        }

        PropsManager propsManager = PluginManager.getPropsManager();

        List<PropsCard> cardS = propsManager.getPropsByUserFromCode(userInfo, Constant.SIGN_DOUBLE_SINGLE_CARD, PropsCard.class);

        boolean doubleStatus = false;
        for (PropsCard card : cardS) {
            if (card.isStatus()) {
                doubleStatus = true;
                if (!propsManager.deleteProp(userInfo, card, PropsCard.class)) {
                    doubleStatus = false;
                    subject.sendMessage("双倍签到金币卡使用失败!");
                }
                break;
            }
        }

        if (doubleStatus) {
            goldNumber = goldNumber * 2;
        }

        if (!EconomyUtil.addMoneyToUser(user, goldNumber)) {
            subject.sendMessage("签到失败!");
            //todo 签到失败回滚
            return;
        }
        double moneyBytUser = EconomyUtil.getMoneyByUser(user);
        messages.append(new PlainText("签到成功!"));
        messages.append(new PlainText(String.format("金币:%s(+%s)", moneyBytUser, goldNumber)));
        if (userInfo.getOldSignNumber() != 0) {
            messages.append(String.format("你的连签线断在了%d天,可惜~", userInfo.getOldSignNumber()));
        }
        if (plainText != null) {
            messages.append(plainText);
        }

        sendSignImage(userInfo, user, subject, messages.build());
//        sendSignImage(userInfo, user, subject, moneyBytUser, goldNumber, messages.build());

//        subject.sendMessage(messages.build());
    }

    /**
     * 发送签到图片信息<p>
     * 在基础信息上添加签到信息<p>
     *
     * @param userInfo 用户信息
     * @param user     用户
     * @param subject  发送者
     * @param messages 消息
     * @author Moyuyanli
     * @date 2022/12/5 16:22
     */
    public static void sendSignImage(UserInfo userInfo, User user, Contact subject, MessageChain messages) {
        BufferedImage userInfoImageBase = UserManager.getUserInfoImageBase(userInfo, user);
        if (userInfoImageBase == null) {
            return;
        }
        Graphics2D graphics = userInfoImageBase.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int fontSize = 20;
        graphics.setColor(Color.black);
        AtomicInteger x = new AtomicInteger(210);
        graphics.setFont(new Font("黑体", Font.PLAIN, fontSize));
        messages.forEach(v -> {
            //写入签到信息
            graphics.drawString(v.contentToString(), 520, x.get());
            x.addAndGet(28);
        });
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
            Graphics2D pen = image.createGraphics();
            //图片与文字的抗锯齿
            pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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
     * @param
     * @return void
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
