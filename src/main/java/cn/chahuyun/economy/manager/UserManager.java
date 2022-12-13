package cn.chahuyun.economy.manager;

import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.util.EconomyUtil;
import cn.chahuyun.economy.util.HibernateUtil;
import cn.chahuyun.economy.util.Log;
import cn.hutool.core.date.DateUtil;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * 用户管理<p>
 * 用户的添加|查询<p>
 * 背包的查看<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 16:20
 */
public class UserManager {

    private static int index = 1;

    private UserManager() {

    }

    /**
     * 获取用户信息<p>
     * 没有的时候自动新建用户<p>
     *
     * @param user 用户
     * @return cn.chahuyun.entity.UserInfo
     * @author Moyuyanli
     * @date 2022/11/14 17:08
     */
    public static UserInfo getUserInfo(User user) {
        long userId = user.getId();
        //查询用户
        try {
            return HibernateUtil.factory.fromTransaction(session -> {
                HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
                JpaCriteriaQuery<UserInfo> query = builder.createQuery(UserInfo.class);
                JpaRoot<UserInfo> from = query.from(UserInfo.class);
                query.select(from);
                query.where(builder.equal(from.get("qq"), userId));
                return session.createQuery(query).getSingleResult().setUser(user);
            });
        } catch (Exception e) {
            //注册用户
            long group = 0;
            if (user instanceof Member) {
                Member member = (Member) user;
                group = member.getGroup().getId();
            }
            UserInfo info = new UserInfo(userId, group, user.getNick(), new Date());
            try {
                return HibernateUtil.factory.fromTransaction(session -> session.merge(info)).setUser(user);
            } catch (Exception exception) {
                Log.error("用户管理错误:注册用户失败", e);
                return getUserInfo(user);
            }
        }
    }


    /**
     * 查询个人信息<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/23 9:37
     */
    public static void getUserInfo(MessageEvent event) {
        Contact subject = event.getSubject();
        User sender = event.getSender();

        UserInfo userInfo = getUserInfo(sender);
        double moneyByUser = EconomyUtil.getMoneyByUser(sender);

        MessageChainBuilder singleMessages = new MessageChainBuilder();

        try {
            Image image = Contact.uploadImage(subject, new URL(sender.getAvatarUrl(AvatarSpec.LARGE)).openConnection().getInputStream());
            singleMessages.append(image);
        } catch (IOException e) {
            Log.error("用户管理:查询个人信息上传图片出错!", e);
        }
        if (userInfo == null) {
            subject.sendMessage("获取用户信息出错!");
            return;
        }

        singleMessages.append(userInfo.getString()).append(String.format("金币:%s", moneyByUser));

        BufferedImage userInfoImageBase = getUserInfoImageBase(userInfo);
        if (userInfoImageBase == null) {
            subject.sendMessage(singleMessages.build());
            return;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(userInfoImageBase, "png", stream);
        } catch (IOException e) {
            Log.error("签到管理:签到图片发送错误!", e);
            subject.sendMessage(singleMessages.build());
            return;
        }
        Contact.sendImage(subject, new ByteArrayInputStream(stream.toByteArray()));

    }


    /**
     * 绘制个人信息基础信息<p>
     * 包含:<p>
     * 头像、名称、id、签到时间、连签次数<p>
     * 下次签到时间、称号<p>
     * 总金币、今日签到获得<p>
     *
     * @param userInfo 用户信息
     * @return java.awt.image.BufferedImage
     * @author Moyuyanli
     * @date 2022/12/5 16:11
     */
    public static BufferedImage getUserInfoImageBase(UserInfo userInfo) {
        //插件的唯一实例
        HuYanEconomy instance = HuYanEconomy.INSTANCE;
        User user = userInfo.getUser();
        try {
            //轮询获取底图
            InputStream asStream = instance.getResourceAsStream("sign" + (index % 4 == 0 ? 4 : index % 4) + ".png");
            index++;
            //验证
            if (asStream == null) {
                Log.error("用户管理:个人信息图片底图获取错误!");
                return null;
            }
            //转图片处理
            BufferedImage image = ImageIO.read(asStream);
            //创建画笔
            Graphics2D pen = image.createGraphics();
            //图片与文字的抗锯齿
            pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //获取头像链接
            String avatarUrl = user.getAvatarUrl(AvatarSpec.LARGE);
            BufferedImage avatar = ImageIO.read(new URL(avatarUrl));
            //圆角处理
            BufferedImage avatarRounder = makeRoundedCorner(avatar, 50);
            //写入头像
            pen.drawImage(avatarRounder, 24, 25, null);

            String userInfoName = userInfo.getName();
            int fontSize;
            //如果是群 根据管理员信息改变颜色
            if (user instanceof Member) {
                MemberPermission permission = ((Member) user).getPermission();
                if (permission == MemberPermission.OWNER) {
                    pen.setColor(Color.YELLOW);
                } else if (permission == MemberPermission.ADMINISTRATOR) {
                    pen.setColor(Color.GREEN);
                } else {
                    pen.setColor(Color.WHITE);
                }
            }
            //根据名字长度改变大小
            if (userInfoName.length() > 6) {
                fontSize = 40;
            } else {
                fontSize = 60;
            }
            /*
             * WHITE(白色)、LIGHT_GRAY（浅灰色）、GRAY（灰色）、DARK_GRAY（深灰色）、
             * BLACK（黑色）、RED（红色）、PINK（粉红色）、ORANGE（橘黄色）、YELLOW（黄色）、
             * GREEN（绿色）、MAGENTA（紫红色）、CYAN（青色）、BLUE（蓝色）
             * 如果上面颜色都不满足你，或者你还想设置下字体透明度，你可以改为如下格式：
             * pen.setColor(new Color(179, 250, 233, 100));
             * Font.PLAIN（正常），Font.BOLD（粗体），Font.ITALIC（斜体）
             */
            // 设置画笔字体样式为黑体，粗体
            pen.setFont(new Font("黑体", Font.BOLD, fontSize));
            pen.drawString(userInfoName, 200, 155);

//            pen.setColor(); todo 称号预留

            pen.setColor(Color.black);
            fontSize = 24;
            pen.setFont(new Font("黑体", Font.PLAIN, fontSize));
            //id
            pen.drawString(String.valueOf(userInfo.getQq()), 172, 240);
            String format;
            String toSing;
            if (userInfo.getSignTime() == null) {
                format = "暂未签到";
                toSing = "暂未签到";
            } else {
                format = DateUtil.format(userInfo.getSignTime(), "yyyy-MM-dd HH:mm:ss");
                toSing = DateUtil.format(DateUtil.offsetDay(userInfo.getSignTime(), 1), "yyyy-MM-dd HH:mm:ss");
            }
            //签到时间
            pen.drawString(format, 172, 320);
            //连签次数
            pen.drawString(String.valueOf(userInfo.getSignNumber()), 172, 360);
            //下次签到时间
            pen.drawString(toSing, 221, 402);
            //其他称号
            pen.drawString("暂无", 172, 440);

            double money = EconomyUtil.getMoneyByUser(user);
            //写入金币
            if (String.valueOf(money).length() > 5) {
                fontSize = 20;
            }
            pen.setFont(new Font("黑体", Font.PLAIN, fontSize));
            //写入总金币
            pen.drawString(String.valueOf(money), 600, 410);

            fontSize = 24;
            pen.setFont(new Font("黑体", Font.PLAIN, fontSize));
            //写入今日获得
            pen.drawString(String.valueOf(userInfo.getSignEarnings()), 810, 410);

            fontSize = 15;
            pen.setColor(new Color(255, 255, 255, 30));
            pen.setFont(new Font("黑体", Font.ITALIC, fontSize));

            pen.drawString("by HuYanEconomy & 壶言经济 " + HuYanEconomy.version, 580, 525);

            //关闭窗体，释放部分资源
            pen.dispose();
            return image;
        } catch (IOException exception) {
            Log.error("用户管理:个人信息基础信息绘图错误!", exception);
            return null;
        }
    }


    /**
     * 圆角处理
     *
     * @param image        BufferedImage 需要处理的图片
     * @param cornerRadius 圆角度
     * @return 处理后的图片
     */
    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();

        output = g2.getDeviceConfiguration().createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        g2.dispose();
        g2 = output.createGraphics();
        /*
        这里绘画圆角矩形
        原图切圆边角
         */
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);
        g2.setComposite(AlphaComposite.SrcIn);
        /*结束*/


        /*这里绘画原型图
        原图切成圆形
         */
//        Ellipse2D.Double shape = new Ellipse2D.Double(0, 0, w, h);
//        g2.setClip(shape);
        /*结束*/

        g2.drawImage(image, 0, 0, w, h, null);
        g2.dispose();

        return output;
    }


}
