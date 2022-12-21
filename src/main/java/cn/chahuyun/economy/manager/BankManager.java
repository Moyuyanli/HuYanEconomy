package cn.chahuyun.economy.manager;

import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;

/**
 * 银行管理<p>
 * 存款|取款<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:26
 */
public class BankManager {

    private BankManager() {
    }

    /**
     * 初始化银行<p>
     * 应当开启银行利息定时器<p>
     *
     * @author Moyuyanli
     * @date 2022/12/21 11:03
     */
    public static void init() {

    }

    /**
     * 存款<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/21 11:04
     */
    public static void deposit(MessageEvent event) {
        UserInfo userInfo = UserManager.getUserInfo(event.getSender());
        User user = userInfo.getUser();

        Contact subject = event.getSubject();

        MessageChain message = event.getMessage();
        MessageChainBuilder singleMessages = MessageUtil.quoteReply(message);
        String code = message.serializeToMiraiCode();


        int money = Integer.parseInt(code.split(" ")[1]);
        double moneyByUser = EconomyUtil.getMoneyByUser(user);
        if (moneyByUser - money <= 0) {
            singleMessages.append(String.format("你的金币不够%s了", money));
            subject.sendMessage(singleMessages.build());
            return;
        }

        if (EconomyUtil.turnUserToBank(user, money)) {
            singleMessages.append("存款成功!");
            subject.sendMessage(singleMessages.build());
        } else {
            singleMessages.append("存款失败!");
            subject.sendMessage(singleMessages.build());
            Log.error("银行管理:存款失败!");
        }
    }

    /**
     * 取款<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/21 11:04
     */
    public static void withdrawal(MessageEvent event) {
        UserInfo userInfo = UserManager.getUserInfo(event.getSender());
        User user = userInfo.getUser();

        Contact subject = event.getSubject();

        MessageChain message = event.getMessage();
        MessageChainBuilder singleMessages = MessageUtil.quoteReply(message);
        String code = message.serializeToMiraiCode();


        int money = Integer.parseInt(code.split(" ")[1]);
        double moneyByBank = EconomyUtil.getMoneyByBank(user);
        if (moneyByBank - money <= 0) {
            singleMessages.append(String.format("你的银行余额不够%s枚金币了", money));
            subject.sendMessage(singleMessages.build());
            return;
        }

        if (EconomyUtil.turnBankToUser(user, money)) {
            singleMessages.append("取款成功!");
            subject.sendMessage(singleMessages.build());
        } else {
            singleMessages.append("取款失败!");
            subject.sendMessage(singleMessages.build());
            Log.error("银行管理:取款失败!");
        }
    }


}
