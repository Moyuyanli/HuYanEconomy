package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.prop.PropBase;
import cn.chahuyun.economy.prop.PropsManager;
import cn.chahuyun.economy.prop.PropsShop;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.*;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 道具商店事件
 *
 * @author Moyuyanli
 * @date 2024/9/25 10:40
 */
@EventComponent
public class EventPropsManager {


    @MessageAuthorize(text = "道具商店( \\d+)?", messageMatching = MessageMatchingEnum.REGULAR)
    public void viewShop(GroupMessageEvent event) {
        String content = event.getMessage().contentToString();

        if (Pattern.matches("道具商店( \\d+)", content)) {
            int i = Integer.parseInt(content.split(" ")[1]);
            viewShop(event, i);
        } else {
            viewShop(event, 1);
        }
    }

    public void viewShop(GroupMessageEvent event, int page) {
        Bot bot = event.getBot();
        Group group = event.getSubject();

        // 获取所有商店信息
        Map<String, String> shopInfo = PropsShop.getShopInfo();

        int pageSize = 10;

        // 计算总页数
        int totalItems = shopInfo.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        // 检查请求的页数是否有效
        if (page < 1 || page > totalPages) {
            group.sendMessage("无效的页数: " + page);
            return;
        }

        // 创建转发消息构建器
        ForwardMessageBuilder nodes = new ForwardMessageBuilder(group);

        // 添加标题
        nodes.add(bot, new PlainText("以下是道具商店↓:"));

        // 计算起始索引和结束索引
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);

        // 条数计数器
        int index = 1;

        // 遍历商店信息并添加到转发消息中
        for (Map.Entry<String, String> entry : shopInfo.entrySet()) {
            if (index > endIndex) {
                break;
            }
            if (index >= startIndex + 1) {
                String format = String.format("道具code:%s%n%s", entry.getKey(), entry.getValue());
                nodes.add(bot, new PlainText(format));
            }
            index++;
        }

        // 添加页脚信息
        nodes.add(bot, new PlainText(String.format("当前页数: %d / 总页数: %d ; 总条数: %d", page, totalPages, totalItems)));

        // 发送消息
        group.sendMessage(nodes.build());
    }


    @MessageAuthorize(
            text = "buy( \\S+)+|购买道具( \\S+)+",
            messageMatching = MessageMatchingEnum.REGULAR
    )
    public void buyProp(GroupMessageEvent event) {
        Log.info("购买指令");

        Group group = event.getSubject();

        MessageChain message = event.getMessage();
        String content = message.contentToString();

        String[] split = content.split(" ");

        MessageChainBuilder builder = new MessageChainBuilder();
        builder.add(new QuoteReply(message));
        builder.add("本次购买道具:");


        for (int i = 1; i < split.length; i++) {
            String code = split[i];

            if (!PropsShop.checkPropExist(code)) {
                builder.add(MessageUtil.formatMessage("\n道具 %s 不存在!",code));
                continue;
            }

            PropBase template = PropsShop.getTemplate(code);
            String name = template.getName();

            UserInfo userInfo = UserManager.getUserInfo(event.getSender());

            double money = EconomyUtil.getMoneyByUser(userInfo.getUser());

            int cost = template.getCost();
            if (money < cost) {
                builder.add(MessageUtil.formatMessage("\n道具 %s ,余额不足%d,购买失败!",name,cost));
                continue;
            }


            if (EconomyUtil.minusMoneyToUser(userInfo.getUser(), cost)) {
                long l = PropsManager.addProp(template);

                BackpackManager.addPropToBackpack(userInfo, code, template.getKind(), l);
                builder.add(MessageUtil.formatMessage("\n道具 %s 购买成功!",name));
            } else {
                builder.add(MessageUtil.formatMessage("\n道具 %s 购买失败!",name));
            }
        }

        group.sendMessage(builder.build());
    }

}
